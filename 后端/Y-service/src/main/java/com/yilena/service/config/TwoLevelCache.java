package com.yilena.service.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Caffeine + Redis 两级缓存
 *
 * 架构设计：
 * - L1（Caffeine）：本地缓存，微秒级访问，负责热点数据的超快访问
 * - L2（Redis）：分布式缓存，毫秒级访问，负责多实例间数据共享
 *
 * 读取流程：Caffeine → Redis（命中后回写L1） → DB（由@Cacheable的valueLoader加载）
 *
 * 缓存防护策略：
 * - 穿透：allowNullValues=true，null包装为NullValue缓存，避免重复查库
 * - 击穿：get(key, valueLoader) 使用per-key互斥锁（@Cacheable(sync=true)走此路径），
 *         只允许一个线程重建缓存，其他线程等待
 * - 雪崩：Redis写入时基础TTL + 0~randomTtlSeconds秒随机偏移，打散过期时间
 *
 * 一致性策略：
 * - 写：同时写两级（先Redis后Caffeine）
 * - 删：同时删两级
 * - 兜底：L1 TTL短于L2，最终一致
 */
@Slf4j
public class TwoLevelCache extends AbstractValueAdaptingCache {

    private final String name;
    private final com.github.benmanes.caffeine.cache.Cache<Object, Object> caffeineCache;
    private final RedisTemplate<String, Object> redisTemplate;
    /** Redis键前缀，与Spring Cache默认格式保持一致（cacheName::key） */
    private final String redisKeyPrefix;
    /** L2 Redis基础TTL（秒） */
    private final long redisTtlSeconds;
    /** L2 Redis随机偏移上限（秒），防止大量key同时过期导致雪崩 */
    private final long randomTtlSeconds;

    /** per-key互斥锁，防止缓存击穿 */
    private final ConcurrentHashMap<Object, ReentrantLock> locks = new ConcurrentHashMap<>();

    public TwoLevelCache(String name, RedisTemplate<String, Object> redisTemplate,
                         long caffeineExpireSeconds, long caffeineMaxSize,
                         long redisTtlSeconds, long randomTtlSeconds) {
        super(true); // allowNullValues=true：缓存null值，防穿透
        this.name = name;
        this.redisTemplate = redisTemplate;
        this.redisKeyPrefix = name + "::";
        this.redisTtlSeconds = redisTtlSeconds;
        this.randomTtlSeconds = randomTtlSeconds;
        // L1：W-TinyLFU淘汰算法 + 写入后过期，TTL短于L2保证最终一致
        this.caffeineCache = Caffeine.newBuilder()
                .maximumSize(caffeineMaxSize)
                .expireAfterWrite(Duration.ofSeconds(caffeineExpireSeconds))
                .recordStats()
                .build();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    /**
     * 两级缓存读取：Caffeine（微秒级）→ Redis（毫秒级，命中回写L1）→ 未命中返回null由DB加载
     */
    @Override
    protected Object lookup(Object key) {
        // 1. 先查L1 Caffeine
        Object value = caffeineCache.getIfPresent(key);
        if (value != null) {
            log.info("[L1-HIT] cache={}, key={}", name, key);
            return value;
        }
        // 2. 再查L2 Redis
        value = redisTemplate.opsForValue().get(redisKeyPrefix + key);
        if (value != null) {
            log.info("[L2-HIT+回写L1] cache={}, key={}", name, key);
            caffeineCache.put(key, value);
            return value;
        }
        log.info("[MISS→DB] cache={}, key={}", name, key);
        return null;
    }

    /**
     * 带加载的读取（@Cacheable(sync=true) 走此路径）
     * per-key互斥锁：同一key只允许一个线程回源DB重建缓存，其他线程等待 → 防击穿
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        // 先无锁快速查一次
        Object value = lookup(key);
        if (value != null) {
            return (T) fromStoreValue(value);
        }
        // 未命中，获取per-key锁后双检回源
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        try {
            value = lookup(key);
            if (value != null) {
                return (T) fromStoreValue(value);
            }
            T loaded = valueLoader.call();
            put(key, loaded);
            return loaded;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        } finally {
            lock.unlock();
            // 锁释放后移除，防止锁对象堆积造成内存泄漏
            locks.remove(key, lock);
        }
    }

    /**
     * 写入：先写L2 Redis（基础TTL+随机偏移，防雪崩），再写L1 Caffeine
     */
    @Override
    public void put(Object key, Object value) {
        Object storeValue = toStoreValue(value);
        long ttl = randomTtlSeconds > 0
                ? redisTtlSeconds + ThreadLocalRandom.current().nextLong(randomTtlSeconds + 1)
                : redisTtlSeconds;
        redisTemplate.opsForValue().set(redisKeyPrefix + key, storeValue, ttl, TimeUnit.SECONDS);
        caffeineCache.put(key, storeValue);
    }

    /**
     * 删除：两级同时删除，保证一致性
     */
    @Override
    public void evict(Object key) {
        redisTemplate.delete(redisKeyPrefix + key);
        caffeineCache.invalidate(key);
    }

    /**
     * 清空该cache分区的所有数据
     */
    @Override
    public void clear() {
        Set<String> keys = redisTemplate.keys(redisKeyPrefix + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        caffeineCache.invalidateAll();
    }
}

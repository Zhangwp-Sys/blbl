package com.yilena.service.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 两级缓存管理器（Caffeine + Redis）
 *
 * 职责：
 * - 按缓存名创建并持有 TwoLevelCache 实例
 * - L1（Caffeine）：maximumSize=10000（W-TinyLFU淘汰），写入后2分钟过期
 * - L2（Redis）：默认基础TTL 5分钟 + 0~5分钟随机偏移（防雪崩）
 * - 特殊缓存分区：offline=1天、videoDanmu=7天（保留原RedisCacheConfig语义）
 *
 * 注册方式：在 CacheConfig 中以 @Primary @Bean 注册，覆盖Spring Boot默认的RedisCacheManager
 */
@RequiredArgsConstructor
public class TwoLevelCacheManager implements CacheManager {

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, Cache> cacheMap = new ConcurrentHashMap<>();

    /** L1 Caffeine 配置 */
    private static final long CAFFEINE_EXPIRE_SECONDS = 120;   // 2分钟，短于L2保证最终一致
    private static final long CAFFEINE_MAX_SIZE = 10000;       // W-TinyLFU淘汰，防内存溢出

    /** L2 Redis 默认TTL：基础5分钟 + 0~5分钟随机偏移 */
    private static final long DEFAULT_REDIS_TTL = 300;
    private static final long DEFAULT_RANDOM_TTL = 300;

    /** per-cache 自定义TTL：[基础TTL(秒), 随机偏移上限(秒)] */
    private static final Map<String, long[]> CUSTOM_TTL = Map.of(
            "offline", new long[]{86400, 0},       // 离线聊天记录：1天
            "videoDanmu", new long[]{604800, 0}    // 弹幕数据：7天
    );

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, n -> {
            long[] ttl = CUSTOM_TTL.getOrDefault(n, new long[]{DEFAULT_REDIS_TTL, DEFAULT_RANDOM_TTL});
            return new TwoLevelCache(n, redisTemplate,
                    CAFFEINE_EXPIRE_SECONDS, CAFFEINE_MAX_SIZE, ttl[0], ttl[1]);
        });
    }

    @Override
    public Collection<String> getCacheNames() {
        return Set.copyOf(cacheMap.keySet());
    }
}

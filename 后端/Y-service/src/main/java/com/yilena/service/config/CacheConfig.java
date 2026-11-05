package com.yilena.service.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 缓存配置类（Caffeine + Redis 两级缓存）
 *
 * 功能说明：
 * 1. 注册两级缓存管理器 TwoLevelCacheManager，覆盖Spring Boot默认的RedisCacheManager
 * 2. 启用Spring缓存注解支持（@Cacheable / @CacheEvict 等）
 *
 * 缓存架构：
 * - L1（Caffeine）：本地缓存，微秒级，TTL 2分钟
 * - L2（Redis）：分布式缓存，毫秒级，默认TTL 5分钟 + 0~5分钟随机偏移
 * - 特殊分区：offline=1天、videoDanmu=7天
 *
 * 缓存防护：
 * - 穿透：空值缓存（AbstractValueAdaptingCache 自动包装NullValue）
 * - 击穿：@Cacheable(sync=true) 时 per-key 互斥锁
 * - 雪崩：基础TTL + 随机偏移
 *
 * @author Yilena
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 注册两级缓存管理器（@Primary确保覆盖自动配置的CacheManager）
     *
     * @param redisTemplate Redis操作模板
     * @return CacheManager 两级缓存管理器
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisTemplate<String, Object> redisTemplate) {
        return new TwoLevelCacheManager(redisTemplate);
    }
}

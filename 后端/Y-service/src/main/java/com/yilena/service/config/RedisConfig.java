package com.yilena.service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 
 * 功能说明：
 * 1. 配置RedisTemplate模板对象，用于Redis数据操作
 * 2. 配置Redis消息监听容器，支持发布/订阅功能
 * 
 * Redis在本项目中的应用场景：
 * - 缓存热点数据（用户信息、视频信息等）
 * - 存储会话信息和Token
 * - 实现分布式锁
 * - 消息队列（通过发布/订阅模式）
 * - 限制请求频率（限流）
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Configuration
@Slf4j
public class RedisConfig {

    /**
     * 创建RedisTemplate模板对象
     * 
     * 功能：配置Redis操作模板，设置连接工厂和序列化器
     * 序列化策略：
     * - Key使用StringRedisSerializer序列化
     * - Value使用Jackson2JsonRedisSerializer序列化（默认）
     * 
     * @param redisConnectionFactory Redis连接工厂，由Spring Boot自动配置
     * @return RedisTemplate Redis操作模板
     */
    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        log.info("开始创建redis模板对象...");
        RedisTemplate redisTemplate = new RedisTemplate();
        //设置redis的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置redis key的序列化器
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        return redisTemplate;
    }


    /**
     * 配置Redis消息监听容器
     * 
     * 功能：创建Redis消息监听容器，用于接收Redis发布/订阅消息
     * 使用场景：
     * - 实时消息推送
     * - 事件通知
     * - 分布式任务调度
     * 
     * @param connectionFactory Redis连接工厂
     * @return RedisMessageListenerContainer 消息监听容器
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

}
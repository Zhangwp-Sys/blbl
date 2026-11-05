package com.yilena.service.config;
import com.yilena.service.utils.SnowFlake;
import com.yilena.service.utils.SnowFlakeProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 雪花算法配置类
 * 
 * 功能说明：
 * 1. 启用雪花算法配置属性绑定
 * 2. 创建雪花算法ID生成器Bean
 * 
 * 配置属性：
 * - SnowFlakeProperties：雪花算法配置参数
 * - 包含数据中心ID、工作机器ID、序列号等配置
 * 
 * 使用场景：
 * - 分布式系统中生成唯一ID
 * - 数据库主键生成
 * - 业务流水号生成
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Configuration
@EnableConfigurationProperties(SnowFlakeProperties.class)
public class SnowFlakeConfig {

    /**
     * 创建雪花算法ID生成器Bean
     * 
     * 功能：初始化雪花算法实例
     * 业务逻辑：
     * 1. 注入雪花算法配置属性
     * 2. 创建SnowFlake实例
     * 3. 配置数据中心ID和工作机器ID
     * 
     * @param properties 雪花算法配置属性
     * @return SnowFlake 雪花算法实例
     */
    @Bean
    public SnowFlake snowFlake(SnowFlakeProperties properties) {
      return new SnowFlake(properties);
    }

}

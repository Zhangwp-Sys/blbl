package com.yilena.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT配置属性类
 * 
 * 功能说明：
 * 1. 绑定JWT相关配置属性
 * 2. 提供JWT密钥和过期时间配置
 * 
 * 配置项：
 * - jwt.secretKey: JWT密钥
 * - jwt.expire: JWT过期时间（毫秒）
 * 
 * 使用场景：
 * - JWT Token生成时使用密钥签名
 * - JWT Token验证时使用密钥验证签名
 * - 设置Token的过期时间
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "jwt")
@Configuration
public class JwtProperties {

    /**
     * JWT密钥
     * 用于Token签名和验证
     */
    private String secretKey;

    /**
     * JWT过期时间（毫秒）
     * Token在此时间后失效
     */
    private long expire;
}

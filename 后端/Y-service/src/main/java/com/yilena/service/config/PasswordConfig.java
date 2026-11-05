package com.yilena.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码配置类
 * 
 * 功能说明：
 * 1. 配置密码编码器
 * 2. 提供BCrypt加密功能
 * 
 * BCrypt特点：
 * - 单向哈希算法
 * - 自动生成随机盐值
 * - 支持密码强度调整
 * - 防止彩虹表攻击
 * 
 * 使用场景：
 * - 用户注册时加密密码
 * - 用户登录时验证密码
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Configuration
public class PasswordConfig {

    /**
     * 创建BCrypt密码编码器
     * 
     * 功能：提供BCrypt密码加密和验证功能
     * 使用场景：
     * - 用户注册时调用encode()加密密码
     * - 用户登录时调用matches()验证密码
     * 
     * @return BCryptPasswordEncoder 密码编码器
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}

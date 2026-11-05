package com.yilena.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security安全配置类
 * 
 * 安全策略说明：
 * 1. 当前配置采用完全开放的安全策略，所有请求无需认证
 * 2. 关闭CSRF保护，适用于无状态REST API设计
 * 3. 禁用默认表单登录，使用自定义JWT认证
 * 
 * 注意事项：
 * - 此配置适用于开发和测试环境
 * - 生产环境建议启用适当的安全认证机制
 * - 实际业务中通过JWT过滤器实现身份验证
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Configuration
public class SecurityConfig {

    /**
     * 配置HTTP安全过滤链
     * 
     * 功能：定义HTTP请求的安全策略
     * 配置项：
     * - 允许所有请求无需认证访问
     * - 禁用CSRF保护（适用于REST API）
     * - 禁用默认表单登录
     * 
     * @param http HTTP安全构建器
     * @return SecurityFilterChain 安全过滤链
     * @throws Exception 配置过程中的异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .anyRequest().permitAll()  // 允许所有请求无需认证
            )
            .csrf(AbstractHttpConfigurer::disable)  // 关闭CSRF保护
            .formLogin(AbstractHttpConfigurer::disable);  // 禁用默认表单登录

        return http.build();
    }
}
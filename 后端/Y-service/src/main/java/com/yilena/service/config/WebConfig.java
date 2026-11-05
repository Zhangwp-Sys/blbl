package com.yilena.service.config;

import com.yilena.service.dao.UserMapper;
import com.yilena.service.filter.GlobalFilter;
import com.yilena.service.interceptor.GlobalInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 * 
 * 功能说明：
 * 1. 配置全局过滤器
 * 2. 配置拦截器
 * 3. 设置请求拦截规则
 * 
 * 过滤器配置：
 * - GlobalFilter：全局过滤器，处理所有请求
 * - 主要功能：日志记录、请求统计、性能监控等
 * 
 * 拦截器配置：
 * - GlobalInterceptor：全局拦截器
 * - 拦截路径：所有请求（/**）
 * - 排除路径：登录、搜索、聊天等公开接口
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /**
     * 全局拦截器对象
     */
    private final GlobalInterceptor globalInterceptor;

    /**
     * RabbitMQ模板对象
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * Redis模板对象
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 注册全局过滤器
     * 
     * 功能：配置并注册全局过滤器
     * 业务逻辑：
     * 1. 创建过滤器注册Bean
     * 2. 设置过滤器实例
     * 3. 配置拦截URL模式
     * 
     * @return FilterRegistrationBean 过滤器注册Bean
     */
    @Bean
    public FilterRegistrationBean<GlobalFilter> globalFilterRegistration() {
        FilterRegistrationBean<GlobalFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new GlobalFilter(rabbitTemplate, redisTemplate));
        registrationBean.addUrlPatterns("/*");
        return registrationBean;
    }

    /**
     * 配置拦截器
     * 
     * 功能：注册并配置全局拦截器
     * 业务逻辑：
     * 1. 注册全局拦截器
     * 2. 设置拦截路径模式
     * 3. 排除不需要拦截的路径
     * 
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //注册自定义拦截器对象
        registry.addInterceptor(globalInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/user/login,/search/,/chat/");//设置拦截器拦截的请求路径（ /** 表示拦截所有请求）
    }
}
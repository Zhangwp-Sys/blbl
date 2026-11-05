package com.yilena.service;

import com.yilena.service.config.JwtProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * Yilena视频网站后端服务主启动类
 * 
 * 功能说明：
 * 1. 启动Spring Boot应用程序
 * 2. 配置MyBatis Mapper接口扫描路径
 * 3. 启用JWT配置属性绑定
 * 4. 启用Redis缓存功能
 * 5. 启用WebSocket支持实时通信功能
 * 6. 启用注解方式的事务管理
 * 
 * 技术栈：
 * - Spring Boot 3.4.2
 * - MyBatis (数据库ORM)
 * - Redis (缓存)
 * - WebSocket (实时通信)
 * - JWT (身份认证)
 * 
 */
@MapperScan("com.yilena.service.dao")
@EnableConfigurationProperties(JwtProperties.class)
@SpringBootApplication
@EnableCaching
@EnableWebSocket
@EnableTransactionManagement
public class MainApplication {

    /**
     * 应用程序主入口方法
     * 
     * 功能：启动Spring Boot应用程序，初始化所有配置和Bean
     * 
     * @param args 命令行参数，可通过--param=value方式传递配置
     */
    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }
}

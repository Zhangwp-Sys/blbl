package com.yilena.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * WebSocket配置类
 * 
 * 功能说明：
 * 1. 注册WebSocket端点导出器
 * 2. 支持WebSocket连接和消息通信
 * 
 * WebSocket在本项目中的应用场景：
 * - 实时聊天功能（用户私信、群聊）
 * - 弹幕实时推送
 * - 实时通知消息
 * - 在线用户状态同步
 * 
 * 技术实现：
 * - 使用@ServerEndpoint注解定义WebSocket端点
 * - 支持JSON格式消息传输
 * - 集成Spring容器管理
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Configuration
public class WebSocketConfig {

    /**
     * 创建WebSocket端点导出器
     * 
     * 功能：注册WebSocket服务器端点，扫描@ServerEndpoint注解的类
     * 作用：使WebSocket端点能够被Servlet容器识别和管理
     * 
     * @return ServerEndpointExporter WebSocket端点导出器
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}
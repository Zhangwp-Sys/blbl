package com.yilena.service.webSocket;

import com.alibaba.fastjson.JSON;
import com.yilena.service.constant.MessageTypeConstant;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.entity.po.ChatMessage;
import com.yilena.service.utils.SnowFlake;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 聊天WebSocket端点
 * 
 * 功能说明：
 * 1. 处理实时聊天消息
 * 2. 管理WebSocket连接
 * 3. 支持私聊和群聊功能
 * 4. 处理离线消息存储
 * 
 * WebSocket端点：
 * - 路径：/YVW/chat/{userId}
 * - 协议：WebSocket
 * - 传输格式：JSON
 * 
 * 消息处理逻辑：
 * - 在线消息：直接转发给接收者
 * - 离线消息：存储到Redis，设置过期时间
 * - 群聊消息：遍历接收者列表，逐个发送
 * - 消息持久化：通过RabbitMQ异步存储
 * 
 * 使用场景：
 * - 用户私信
 * - 群组聊天
 * - 实时通知
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Slf4j
@ServerEndpoint("/YVW/chat/{userId}")
@Component
public class ChatEndpoint {

    /**
     * Redis模板对象
     */
    private static RedisTemplate<String, Object> redisTemplate;

    /**
     * RabbitMQ模板对象
     */
    private static RabbitTemplate rabbitTemplate;

    /**
     * 雪花算法ID生成器
     */
    @Autowired
    private SnowFlake snowFlake;

    /**
     * 无参构造函数
     * 必须显式定义，WebSocket端点需要
     */
    public ChatEndpoint() {
        log.info("ChatEndpoint无参构造函数被调用");
    }

    /**
     * 设置Redis模板对象
     * 
     * @param redisTemplate Redis模板对象
     */
    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        ChatEndpoint.redisTemplate = redisTemplate;
    }

    /**
     * 设置RabbitMQ模板对象
     * 
     * @param rabbitTemplate RabbitMQ模板对象
     */
    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        ChatEndpoint.rabbitTemplate = rabbitTemplate;
    }

    /**
     * WebSocket连接建立事件
     * 
     * 功能：处理用户WebSocket连接建立
     * 业务逻辑：
     * 1. 解析用户ID
     * 2. 存储用户ID到Session属性
     * 3. 注册到WebSocket会话管理器
     * 4. 记录连接日志
     * 
     * @param session WebSocket会话
     * @param userIdStr 用户ID字符串
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userIdStr) {
        Long userId = Long.valueOf(userIdStr);
        session.getUserProperties().put("userId", userId); // 存储用户ID到Session属性
        WebSocketSessionManager.addSession(Long.valueOf(userId), session);
        log.info("用户 {} 连接建立，当前在线人数：{}", userId, WebSocketSessionManager.getOnlineCount());
    }

    /**
     * 接收到WebSocket消息
     * 
     * 功能：处理接收到的WebSocket消息
     * 业务逻辑：
     * 1. 记录接收到的消息
     * 2. 可扩展为消息预处理
     * 
     * @param chatMessageJson 消息JSON字符串
     * @param sid 用户ID字符串
     */
    @OnMessage
    public void onMessage(String chatMessageJson, @PathParam("userId") String sid){
        log.info("用户 {} 发送消息：{}", sid, chatMessageJson);
    }

    /**
     * 发送消息
     * 
     * 功能：向指定用户发送消息
     * 业务逻辑：
     * 1. 解析消息对象
     * 2. 设置消息状态
     * 3. 根据消息类型处理：
     *    - 视频消息：遍历接收者列表发送
     *    - 私聊消息：直接转发给接收者
     * 4. 处理离线消息存储
     * 5. 通过RabbitMQ持久化消息
     * 
     * @param chatMessageJson 消息JSON字符串
     */
    public void sendMessage(String chatMessageJson) {
        ChatMessage message = JSON.parseObject(chatMessageJson, ChatMessage.class);
        message.setIsExpired(StatusConstant.STATUS_NO);
        log.info("用户{}发送消息：{}，给{}", message.getSendUserId(), message.getContent(), message.getReceiveUserId());

        if(Objects.equals(message.getType(), MessageTypeConstant.VIDEO)){
            message.getReceiveUserIds().forEach(receiveUserId -> {
                message.setId(snowFlake.getID());
                message.setReceiveUserId(receiveUserId);
                Session receiverSession = WebSocketSessionManager.getSession(receiveUserId);
                if (receiverSession != null) {
                    receiverSession.getAsyncRemote().sendText(chatMessageJson);
                }
                // 发送消息到RabbitMQ
                rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.CHAT_BINDING_KEY, message);
            });
        }else {
            // 转发给接收者
            Session receiverSession = WebSocketSessionManager.getSession(message.getReceiveUserId());
            if (receiverSession != null) {
                receiverSession.getAsyncRemote().sendText(chatMessageJson);
            }

                if(!Objects.equals(message.getStatus(),StatusConstant.STATUS_WAIT)){
                // 离线时按发送方维度存储
                String redisKey = "offline:" + message.getReceiveUserId() + ":" + message.getSendUserId();
                redisTemplate.opsForList().rightPush(redisKey, chatMessageJson);
                // 设置过期时间为一天
                redisTemplate.expire(redisKey, Duration.ofDays(1));
                }

        }

        if (!Objects.equals(message.getStatus(), StatusConstant.STATUS_WAIT) && !Objects.equals(message.getType(), MessageTypeConstant.VIDEO) ) {
            // 发送消息到RabbitMQ
            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.CHAT_BINDING_KEY, message);
        }

    }

    /**
     * WebSocket连接关闭事件
     * 
     * 功能：处理用户WebSocket连接关闭
     * 业务逻辑：
     * 1. 获取用户ID
     * 2. 从会话管理器中移除
     * 3. 记录关闭日志
     * 
     * @param session WebSocket会话
     */
    @OnClose
    public void onClose(Session session) {
        Long userId = (Long) session.getUserProperties().get("userId");
        WebSocketSessionManager.removeSession(userId);
        log.info("用户 {} 连接关闭，剩余在线人数：{}", userId, WebSocketSessionManager.getOnlineCount());
    }

    /**
     * WebSocket错误处理
     * 
     * 功能：处理WebSocket连接错误
     * 业务逻辑：
     * 1. 获取用户ID
     * 2. 记录错误日志
     * 
     * @param session WebSocket会话
     * @param error 错误信息
     */
    @OnError
    public void onError(Session session, Throwable error) {
        Integer userId = (Integer) session.getUserProperties().get("userId");
        log.error("用户 {} 的WebSocket发生错误：{}", userId, error.getMessage());
    }
}

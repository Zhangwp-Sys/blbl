package com.yilena.service.config;

import com.yilena.service.constant.MqConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ配置类
 * 
 * 功能说明：
 * 1. 配置RabbitMQ消息转换器
 * 2. 配置消息监听容器
 * 3. 配置交换机、队列和绑定关系
 * 
 * 消息队列设计：
 * - 采用直连交换机（Direct Exchange）
 * - 每个业务模块独立队列
 * - 通过绑定键进行路由
 * 
 * 业务队列：
 * - 用户队列：用户相关操作
 * - 关注队列：关注关系变更
 * - 收藏队列：收藏操作
 * - 视频队列：视频相关操作
 * - 帖子队列：帖子相关操作
 * - 聊天队列：聊天消息
 * - 历史队列：浏览历史
 * - 热搜队列：热搜数据
 * - 视频收藏夹队列：视频收藏夹操作
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class RabbitMqConfig {

    /**
     * 创建JSON消息转换器
     * 
     * 功能：配置RabbitMQ消息的JSON序列化
     * 使用场景：消息发送和接收时的格式转换
     * 
     * @return MessageConverter 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 创建RabbitMQ模板对象
     * 
     * 功能：配置RabbitMQ消息发送模板
     * 业务逻辑：
     * 1. 创建RabbitTemplate实例
     * 2. 设置JSON消息转换器
     * 3. 注册生产者确认回调（ConfirmCallback）：确认消息是否到达交换机
     * 4. 注册路由失败回调（ReturnsCallback）：消息无法路由到队列时触发
     * 
     * @param connectionFactory RabbitMQ连接工厂
     * @return RabbitTemplate RabbitMQ模板对象
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        // 生产者确认回调：ack=true 表示消息已到达交换机
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息未到达交换机，原因：{}", cause);
            }
        });
        // 路由失败回调：消息到达交换机但无法路由到任何队列时触发（需开启 publisher-returns）
        template.setReturnsCallback(returned -> log.error(
                "消息路由失败：exchange={}, routingKey={}, replyText={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyText()));
        return template;
    }

    /**
     * 创建消息监听容器工厂
     * 
     * 功能：配置消息监听容器
     * 业务逻辑：
     * 1. 创建监听容器工厂
     * 2. 设置连接工厂
     * 3. 配置JSON消息转换器
     * 
     * @param connectionFactory RabbitMQ连接工厂
     * @return SimpleRabbitListenerContainerFactory 监听容器工厂
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        // 关键：自定义工厂会覆盖Spring Boot自动配置，yaml中的acknowledge-mode不会自动应用，
        // 必须显式设置为手动ACK，否则容器仍按AUTO模式自动确认，手动basicAck会导致重复ACK（406 unknown delivery tag）
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        return factory;
    }

    /**
     * 创建直连交换机
     * 
     * 功能：配置RabbitMQ直连交换机
     * 使用场景：根据路由键精确匹配队列
     * 
     * @return DirectExchange 直连交换机
     */
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(MqConstant.CRUD_EXCHANGE);
    }

    /**
     * 创建用户队列
     * 
     * 功能：配置用户业务队列
     * 使用场景：处理用户注册、登录、信息修改等操作
     * 
     * @return Queue 用户队列
     */
    @Bean
    public Queue userQueue() {
        return new Queue(MqConstant.USER_QUEUE);
    }

    /**
     * 创建关注队列
     * 
     * 功能：配置关注业务队列
     * 使用场景：处理用户关注、取消关注等操作
     * 
     * @return Queue 关注队列
     */
    @Bean
    public Queue followQueue() {
        return new Queue(MqConstant.FOLLOW_QUEUE);
    }

    /**
     * 创建收藏队列
     * 
     * 功能：配置收藏业务队列
     * 使用场景：处理视频、帖子等收藏操作
     * 
     * @return Queue 收藏队列
     */
    @Bean
    public Queue collectionQueue() {
        return new Queue(MqConstant.COLLECTION_QUEUE);
    }

    /**
     * 创建收藏夹队列
     * 
     * 功能：配置收藏夹业务队列
     * 使用场景：处理收藏夹创建、修改、删除等操作
     * 
     * @return Queue 收藏夹队列
     */
    @Bean
    public Queue favoritesQueue() {
        return new Queue(MqConstant.FAVORITES_QUEUE);
    }

    /**
     * 创建视频队列
     * 
     * 功能：配置视频业务队列
     * 使用场景：处理视频上传、审核、播放等操作
     * 
     * @return Queue 视频队列
     */
    @Bean
    public Queue videoQueue() {
        return new Queue(MqConstant.VIDEO_QUEUE);
    }

    /**
     * 创建帖子队列
     * 
     * 功能：配置帖子业务队列
     * 使用场景：处理帖子发布、审核、删除等操作
     * 
     * @return Queue 帖子队列
     */
    @Bean
    public Queue postQueue() {
        return new Queue(MqConstant.POST_QUEUE);
    }

    /**
     * 创建聊天队列
     * 
     * 功能：配置聊天业务队列
     * 使用场景：处理私信、群聊等消息
     * 
     * @return Queue 聊天队列
     */
    @Bean
    public Queue chatQueue() {
        return new Queue(MqConstant.CHAT_QUEUE);
    }

    /**
     * 创建历史记录队列
     * 
     * 功能：配置历史记录业务队列
     * 使用场景：处理浏览历史记录的存储和查询
     * 
     * @return Queue 历史记录队列
     */
    @Bean
    public Queue historyQueue() {
        return new Queue(MqConstant.HISTORY_QUEUE);
    }

    /**
     * 创建热搜队列
     * 
     * 功能：配置热搜业务队列
     * 使用场景：处理热搜数据的更新和查询
     * 
     * @return Queue 热搜队列
     */
    @Bean
    public Queue hotSearchQueue() {
        return new Queue(MqConstant.HOT_SEARCH_QUEUE);
    }

    /**
     * 创建视频收藏夹队列
     * 
     * 功能：配置视频收藏夹业务队列
     * 使用场景：处理视频与收藏夹的关联操作
     * 
     * @return Queue 视频收藏夹队列
     */
    @Bean
    public Queue videoFavoritesQueue() {
        return new Queue(MqConstant.VIDEO_FAVORITES_QUEUE);
    }

    /**
     * 创建用户队列绑定
     * 
     * 功能：将用户队列绑定到直连交换机
     * 业务逻辑：
     * 1. 绑定用户队列到交换机
     * 2. 设置路由键
     * 
     * @param directExchange 直连交换机
     * @param userQueue 用户队列
     * @return Binding 绑定关系
     */
    @Bean
    public Binding UserBinding(DirectExchange directExchange, Queue userQueue) {
        return BindingBuilder.bind(userQueue)
                .to(directExchange)
                .with(MqConstant.USER_BINDING_KEY);
    }

    /**
     * 创建关注队列绑定
     * 
     * 功能：将关注队列绑定到直连交换机
     * 
     * @param directExchange 直连交换机
     * @param followQueue 关注队列
     * @return Binding 绑定关系
     */
    @Bean
    public Binding FollowBinding(DirectExchange directExchange, Queue followQueue) {
        return BindingBuilder.bind(followQueue)
                .to(directExchange)
                .with(MqConstant.FOLLOW_BINDING_KEY);
    }

    /**
     * 创建收藏队列绑定
     * 
     * 功能：将收藏队列绑定到直连交换机
     * 
     * @param directExchange 直连交换机
     * @param collectionQueue 收藏队列
     * @return Binding 绑定关系
     */
    @Bean
    public Binding CollectionBinding(DirectExchange directExchange, Queue collectionQueue) {
        return BindingBuilder.bind(collectionQueue)
                .to(directExchange)
                .with(MqConstant.COLLECTION_BINDING_KEY);
    }

    /**
     * 创建收藏夹队列绑定
     * 
     * 功能：将收藏夹队列绑定到直连交换机
     * 
     * @param directExchange 直连交换机
     * @param favoritesQueue 收藏夹队列
     * @return Binding 绑定关系
     */
    @Bean
    public Binding FavoritesBinding(DirectExchange directExchange, Queue favoritesQueue) {
        return BindingBuilder.bind(favoritesQueue)
                .to(directExchange)
                .with(MqConstant.FAVORITES_BINDING_KEY);
    }

    /**
     * 创建视频队列绑定
     * 
     * 功能：将视频队列绑定到直连交换机
     * 
     * @param directExchange 直连交换机
     * @param videoQueue 视频队列
     * @return Binding 绑定关系
     */
    @Bean
    public Binding VideoBinding(DirectExchange directExchange, Queue videoQueue) {
        return BindingBuilder.bind(videoQueue)
                .to(directExchange)
                .with(MqConstant.VIDEO_BINDING_KEY);
    }

    /**
     * 创建帖子队列绑定
     * 
     * 功能：将帖子队列绑定到直连交换机
     * 
     * @param directExchange 直连交换机
     * @param postQueue 帖子队列
     * @return Binding 绑定关系
     */
    @Bean
    public Binding PostBinding(DirectExchange directExchange, Queue postQueue) {
        return BindingBuilder.bind(postQueue)
                .to(directExchange)
                .with(MqConstant.POST_BINDING_KEY);
    }

    /**
     * 创建聊天队列绑定
     * 
     * 功能：将聊天队列绑定到直连交换机
     * 
     * @param directExchange 直连交换机
     * @param chatQueue 聊天队列
     * @return Binding 绑定关系
     */
    @Bean
    public Binding ChatBinding(DirectExchange directExchange, Queue chatQueue) {
        return BindingBuilder.bind(chatQueue)
                .to(directExchange)
                .with(MqConstant.CHAT_BINDING_KEY);
    }

    /**
     * 创建历史记录队列绑定
     * 
     * 功能：将历史记录队列绑定到直连交换机
     * 
     * @param directExchange 直连交换机
     * @param historyQueue 历史记录队列
     * @return Binding 绑定关系
     */
    @Bean
    public Binding HistoryBinding(DirectExchange directExchange, Queue historyQueue) {
        return BindingBuilder.bind(historyQueue)
                .to(directExchange)
                .with(MqConstant.HISTORY_BINDING_KEY);
    }

    /**
     * 创建热搜队列绑定
     * 
     * 功能：将热搜队列绑定到直连交换机
     * 
     * @param directExchange 直连交换机
     * @param hotSearchQueue 热搜队列
     * @return Binding 绑定关系
     */
    @Bean
    public Binding HotSearchBinding(DirectExchange directExchange, Queue hotSearchQueue) {
        return BindingBuilder.bind(hotSearchQueue)
                .to(directExchange)
                .with(MqConstant.HOT_SEARCH_BINDING_KEY);
    }

    /**
     * 创建视频收藏夹队列绑定
     * 
     * 功能：将视频收藏夹队列绑定到直连交换机
     * 
     * @param directExchange 直连交换机
     * @param videoFavoritesQueue 视频收藏夹队列
     * @return Binding 绑定关系
     */
    @Bean
    public Binding VideoFavoritesBinding(DirectExchange directExchange, Queue videoFavoritesQueue) {
        return BindingBuilder.bind(videoFavoritesQueue)
                .to(directExchange)
                .with(MqConstant.VIDEO_FAVORITES_BINDING_KEY);
    }
}

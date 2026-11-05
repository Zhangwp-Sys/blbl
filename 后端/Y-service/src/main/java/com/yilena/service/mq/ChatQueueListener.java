package com.yilena.service.mq;

import com.rabbitmq.client.Channel;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.dao.ChatMessageMapper;
import com.yilena.service.entity.po.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatQueueListener {

    private final ChatMessageMapper chatMessageMapper;

    @RabbitListener(queues = MqConstant.CHAT_QUEUE)
    public void receive(ChatMessage chatMessage, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("接收到消息：{}", chatMessage);
        try {
            chatMessageMapper.addMessage(chatMessage);
            // 处理成功，手动ACK确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("聊天消息落库失败，重新入队", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

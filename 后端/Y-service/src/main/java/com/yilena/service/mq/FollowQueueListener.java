package com.yilena.service.mq;

import com.rabbitmq.client.Channel;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.entity.po.UserFollowing;
import com.yilena.service.es.FollowES;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class FollowQueueListener {

    private final FollowES followES;

    @RabbitListener(queues = MqConstant.FOLLOW_QUEUE)
    public void receive(UserFollowing userFollowing, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("接收到消息：{}", userFollowing);
        try {
            if(Objects.equals(userFollowing.getType(), MqConstant.SEND_ADD)){
                followES.add(userFollowing);
            }
            else if(Objects.equals(userFollowing.getType(), MqConstant.SEND_DELETE)){
                followES.delete(userFollowing);
            }
            // 处理成功，手动ACK确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费关注消息失败，重新入队", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

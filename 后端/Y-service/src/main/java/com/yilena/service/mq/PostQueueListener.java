package com.yilena.service.mq;

import com.rabbitmq.client.Channel;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.entity.vo.PostVO;
import com.yilena.service.es.PostES;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostQueueListener {

    private final PostES postES;

    @RabbitListener(queues = MqConstant.POST_QUEUE)
    public void receive(PostVO postVO, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("动态队列监听到消息:{}", postVO);
        try {
            if(Objects.equals(postVO.getType(), MqConstant.SEND_ADD)) {
                postES.addPost(postVO);
            }
            else if (Objects.equals(postVO.getType(), MqConstant.SEND_PUT)) {
                postES.updatePost(postVO);
            }
            else if (Objects.equals(postVO.getType(), MqConstant.SEND_DELETE)) {
                postES.deletePost(postVO.getId());
            }
            // 处理成功，手动ACK确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费动态消息失败，重新入队", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

package com.yilena.service.mq;

import com.rabbitmq.client.Channel;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.entity.vo.VideoVO;
import com.yilena.service.es.VideoES;
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
public class VideoQueueListener {

    private final VideoES videoES;

    @RabbitListener(queues = MqConstant.VIDEO_QUEUE)
    public void receive(VideoVO videoVO, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("视频队列监听器收到消息：{}", videoVO);
        try {
            if(Objects.equals(videoVO.getType(), MqConstant.SEND_ADD)){
                videoES.addVideo(videoVO);
            }
            else if (Objects.equals(videoVO.getType(),MqConstant.SEND_PUT)) {
                videoES.updateVideo(videoVO);
            }
            else if(Objects.equals(videoVO.getType(), MqConstant.SEND_DELETE)){
                videoES.deleteVideo(videoVO);
            }
            // 处理成功，手动ACK确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费视频消息失败，重新入队", e);
            // 处理失败，NACK重新入队
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

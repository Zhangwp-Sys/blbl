package com.yilena.service.mq;

import com.rabbitmq.client.Channel;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.dao.VideoFavoritesMapper;
import com.yilena.service.entity.po.VideoFavorites;
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
public class VideoFavoritesQueueListener {

    private final VideoFavoritesMapper videoFavoritesMapper;

    @RabbitListener(queues = MqConstant.VIDEO_FAVORITES_QUEUE)
    public void receive(VideoFavorites videoFavorites, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("接收到消息：{}", videoFavorites);
        try {
            if(Objects.equals(videoFavorites.getType(), MqConstant.SEND_ADD)){
                videoFavoritesMapper.addVideoFavorites(videoFavorites);
            }
            else if(Objects.equals(videoFavorites.getType(), MqConstant.SEND_DELETE)){
                videoFavoritesMapper.deleteVideoFavorites(videoFavorites);
            }
            // 处理成功，手动ACK确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费视频收藏消息失败，重新入队", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

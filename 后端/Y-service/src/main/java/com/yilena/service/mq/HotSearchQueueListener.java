package com.yilena.service.mq;

import com.rabbitmq.client.Channel;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.dao.HotSearchMapper;
import com.yilena.service.entity.po.HotSearch;
import com.yilena.service.utils.SnowFlake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class HotSearchQueueListener {

    private final HotSearchMapper hotSearchMapper;
    private final SnowFlake snowFlake;

    @RabbitListener(queues = MqConstant.HOT_SEARCH_QUEUE)
    public void receive(HotSearch hotSearch, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("收到热搜消息：{}", hotSearch);
        try {
            HotSearch hs = hotSearchMapper.getHotSearchVideoByContent(hotSearch.getContent());
            if(hs == null){
                hotSearch.setId(snowFlake.getID());
                hotSearch.setSearchCount(1);
                hotSearch.setCreatedTime(LocalDateTime.now());
                hotSearchMapper.addSearchHistory(hotSearch);
            }
            else{
                hs.setSearchCount(hs.getSearchCount()+1);
                hotSearchMapper.updateSearchHistory(hs);
            }
            // 处理成功，手动ACK确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费热搜消息失败，重新入队", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

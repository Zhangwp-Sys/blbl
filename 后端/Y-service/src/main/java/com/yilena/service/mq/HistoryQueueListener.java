package com.yilena.service.mq;

import com.rabbitmq.client.Channel;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.dao.HistoryMapper;
import com.yilena.service.entity.po.History;
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
public class HistoryQueueListener {

    private final HistoryMapper historyMapper;

    @RabbitListener(queues = MqConstant.HISTORY_QUEUE)
    public void receive(History history, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("接收到消息：{}", history);
        try {
            History oldHistory = historyMapper.getHistoryByUserIdAndEntityId(history.getEntityType(), history.getEntityId(), history.getUserId());
            if(oldHistory != null){
                historyMapper.deleteHistory(oldHistory.getId());
            }
            historyMapper.addHistory(history);
            // 处理成功，手动ACK确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("消费历史记录消息失败，重新入队", e);
            channel.basicNack(deliveryTag, false, true);
        }
    }
}

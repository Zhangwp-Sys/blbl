package com.yilena.service.mq;

import com.rabbitmq.client.Channel;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.constant.UserConstant;
import com.yilena.service.dao.UserMapper;
import com.yilena.service.entity.po.User;
import com.yilena.service.entity.vo.UserVO;
import com.yilena.service.es.UserES;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserQueueListener {

    private final UserES userES;
    private final UserMapper userMapper;

    @RabbitListener(queues = MqConstant.USER_QUEUE)
    public void receive(UserVO userVO, Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) throws IOException {
        log.info("接收到消息：{}", userVO);
        try {
            if(Objects.equals(userVO.getType(), MqConstant.SEND_ADD)){
                userES.addUser(userVO);
            }
            else if(Objects.equals(userVO.getType(), MqConstant.SEND_PUT)){
                userES.updateUser(userVO);
            }else if(Objects.equals(userVO.getType(), MqConstant.SEND_QUERY)){
                User user = userMapper.getUserById(userVO.getId());
                if(Objects.equals(user.getStatus(), StatusConstant.STATUS_NO)){
                    throw new RuntimeException(UserConstant.USER_CAN_NOT_DO_ANYTHING);
                }
            }
            // 处理成功，手动ACK确认
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 仅SEND_QUERY（冻结检查）异常不重投：Filter每个请求都会重发该检查消息，天然具备重试，
            // 若重入队反而会让冻结用户的消息无限循环（毒消息）
            if (Objects.equals(userVO.getType(), MqConstant.SEND_QUERY)) {
                log.warn("冻结检查消息处理异常，确认后不再重投：userId={}, 原因={}", userVO.getId(), e.getMessage());
                channel.basicNack(deliveryTag, false, false);
            } else {
                // ES同步等业务消息失败必须重入队，保证MySQL与ES最终一致
                log.error("消费用户消息失败，重新入队", e);
                channel.basicNack(deliveryTag, false, true);
            }
        }
    }
}

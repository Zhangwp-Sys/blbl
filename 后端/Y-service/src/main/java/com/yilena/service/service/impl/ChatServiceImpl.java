package com.yilena.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.yilena.service.constant.DelMsgConstant;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.dao.ChatMessageMapper;
import com.yilena.service.dao.FollowMapper;
import com.yilena.service.dao.UserMapper;
import com.yilena.service.entity.dto.ReadRequestDTO;
import com.yilena.service.entity.po.ChatMessage;
import com.yilena.service.entity.po.User;
import com.yilena.service.entity.vo.MessageShowVO;
import com.yilena.service.entity.vo.UserVO;
import com.yilena.service.service.ChatService;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.SnowFlake;
import com.yilena.service.webSocket.ChatEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 聊天服务实现类
 * 
 * 功能说明：
 * 1. 实现即时通讯相关的业务逻辑
 * 2. 处理消息的发送、接收、删除、撤回
 * 3. 实现离线消息存储和已读标记
 * 4. 支持多种消息类型的查询
 * 
 * 核心功能：
 * - 消息管理：发送、接收、删除、撤回消息
 * - 离线消息：使用Redis存储离线消息
 * - 已读标记：标记消息为已读
 * - 消息查询：查询回复、@、点赞、系统等消息
 * 
 * 技术架构：
 * - 使用WebSocket实现实时消息推送
 * - 使用Redis存储离线消息
 * - 使用RabbitMQ同步消息数据
 * - 使用事务管理确保数据一致性
 * 
 */
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    /**
     * Redis模板对象
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 聊天消息数据访问对象
     */
    private final ChatMessageMapper chatMessageMapper;

    /**
     * 聊天WebSocket端点
     */
    private final ChatEndpoint chatEndpoint;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 关注数据访问对象
     */
    private final FollowMapper followMapper;

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 获取历史消息
     * 
     * 功能：获取两个用户之间的历史消息
     * 业务逻辑：
     * 1. 从数据库获取历史消息
     * 2. 从Redis获取离线消息
     * 3. 合并并去重消息
     * 4. 过滤掉单方面删除的消息
     * 
     * @param currentUserId 当前用户ID
     * @param targetUserId 目标用户ID
     * @return 合并后的消息列表
     */
    @Transactional
    public List<ChatMessage> getHistoryMessage(Long currentUserId, Long targetUserId) {
        // 1. 从数据库获取历史消息
        List<ChatMessage> dbMessages = chatMessageMapper.getMessageByPair(currentUserId, targetUserId);

        // 2. 从Redis获取该对话的离线消息
        String redisKey = "offline:" + currentUserId + ":" + targetUserId;
        List<Object> redisMessages = redisTemplate.opsForList().range(redisKey, 0, -1);

        // 3. 转换并合并消息
        List<ChatMessage> allMessages = mergeMessages(dbMessages, redisMessages);

//         4. 标记这些消息为已读(错误，不该在此处标记已读)
//        chatMessageMapper.markMessagesAsRead(currentUserId, targetUserId);

        return allMessages;
    }

    /**
     * 标记消息为已读
     * 
     * 功能：将消息标记为已读
     * 业务逻辑：
     * 1. 更新数据库中消息状态为已读
     * 2. 通过WebSocket发送已读通知
     * 3. 清理Redis中的离线消息
     * 4. 更新离线消息过期状态
     * 
     * @param requestDTO 已读请求信息
     */
    @Override
    public void markMessagesAsRead(ReadRequestDTO requestDTO) {
        // 更新所有消息状态为已读
        chatMessageMapper.markMessagesAsRead(requestDTO.getCurrentUserId(), requestDTO.getTargetUserId());

        // 给对方发送消息表示自己已读
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setStatus(StatusConstant.STATUS_WAIT);
        chatMessage.setContent("已读");
        chatMessage.setReceiveUserId(requestDTO.getTargetUserId());
        chatMessage.setSendUserId(requestDTO.getCurrentUserId());

        String json = JSON.toJSONString(chatMessage);
        chatEndpoint.sendMessage(json);

        String redisKey = "offline:" + requestDTO.getCurrentUserId() + ":" + requestDTO.getTargetUserId();
        // 清理Redis中的离线消息
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) {
                operations.watch(redisKey);
                operations.multi();
                operations.delete(redisKey);
                List<Object> results = operations.exec();
                if(results != null && !results.isEmpty()){
                    chatMessageMapper.updateExpiredStatus(requestDTO.getCurrentUserId(), requestDTO.getTargetUserId());
                }
                return results;
            }
        });
    }

    /**
     * 发送消息
     * 
     * 功能：发送聊天消息
     * 业务逻辑：
     * 1. 设置消息默认值（状态、时间、可见性）
     * 2. 生成唯一消息ID
     * 3. 通过WebSocket发送消息
     * 
     * @param chatMessage 聊天消息
     */
    @Transactional
    @Override
    public void sendMessage(ChatMessage chatMessage) {
        chatMessage.setStatus(StatusConstant.STATUS_NO);
        chatMessage.setCreatedTime(LocalDateTime.now());
        chatMessage.setVisibilityBySender(StatusConstant.STATUS_YES);
        chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
        chatMessage.setIsExpired(StatusConstant.STATUS_NO);

        // 生成唯一ID
        chatMessage.setId(snowFlake.getID());
        String json = JSON.toJSONString(chatMessage);
        chatEndpoint.sendMessage(json);
    }

    /**
     * 删除或撤回消息
     * 
     * 功能：删除或撤回聊天消息
     * 业务逻辑：
     * - 删除消息：设置消息对当前用户不可见
     * - 撤回消息：
     *   1. 验证是否为发送者
     *   2. 验证是否在2分钟内
     *   3. 从Redis中移除离线消息
     *   4. 从数据库中删除消息
     *   5. 发送撤回通知
     * 
     * @param messageId 消息ID
     * @param type 操作类型（删除/撤回）
     * @throws RuntimeException 撤回时间超过2分钟时抛出异常
     */
    @Transactional
    @Override
    public void deleteMessage(Long messageId, Integer type) {
        ChatMessage message = chatMessageMapper.getMessageById(messageId);
        // 删除消息
        if(type.equals(DelMsgConstant.DELETE)){
            if(Objects.equals(message.getSendUserId(), CurrentHolder.getCurrent())){
                message.setVisibilityBySender(StatusConstant.STATUS_NO);
            }else{
                message.setVisibilityByReceiver(StatusConstant.STATUS_NO);
            }
            chatMessageMapper.updateMessageById(message);
        }
        // 撤回消息
        else if(type.equals(DelMsgConstant.WITHDRAW)){
            if(Objects.equals(message.getSendUserId(), CurrentHolder.getCurrent())){
                // 如果发送时间小于两分钟
                LocalDateTime validTime = LocalDateTime.now().minusMinutes(2);
                if(message.getCreatedTime().isAfter(validTime)){
                    //删除redis
                    String messageJson = JSON.toJSONString(message);
                    redisTemplate.opsForList().remove("offline:" + message.getSendUserId()
                            + ":" + message.getReceiveUserId(), 0, messageJson);

                    chatMessageMapper.deleteMessage(messageId);

                    // 给对方发送消息表示自己已经撤回
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.setStatus(StatusConstant.STATUS_WAIT);
                    chatMessage.setContent("撤回");
                    chatMessage.setReceiveUserId(message.getReceiveUserId());
                    chatMessage.setSendUserId(message.getSendUserId());

                    String json = JSON.toJSONString(chatMessage);
                    chatEndpoint.sendMessage(json);
                }else{
                    throw new RuntimeException("发送时间大于两分钟，无法撤回");
                }
            }
        }
    }

    /**
     * 获取聊天用户列表
     * 
     * 功能：获取当前用户的聊天用户列表
     * 业务逻辑：
     * 1. 获取关注的用户列表
     * 2. 统计每个用户的未读消息数（Redis + 数据库）
     * 3. 返回用户列表
     * 
     * @param userId 用户ID
     * @return 聊天用户列表
     */
    @Override
    public List<UserVO> getUserList(Long userId) {
        List<UserVO> userVOS = followMapper.getFollowingUsers(userId);
        if(userVOS.isEmpty()){
            return List.of();
        }

        // 获取未读信息个数
        userVOS.forEach(userVO -> {
            String redisKey = "offline:" + userId + ":" + userVO.getId();
            Long count = redisTemplate.opsForList().size(redisKey);
            Long dbCount = chatMessageMapper.getMessageWhichUnReadAndIsExpired(userId, userVO.getId());
            // 避免空指针
            long totalUnread = (count != null ? count : 0L) + (dbCount != null ? dbCount : 0L);
            userVO.setUnReadMessageCount(totalUnread);
        });
        return userVOS;
    }

    /**
     * 获取回复我的消息
     * 
     * @param userId 用户ID
     * @return 回复消息列表
     */
    @Override
    public List<MessageShowVO> getAnswerMe(Long userId) {
        List<MessageShowVO> messageShowVOS = chatMessageMapper.getAnswerMe(userId);
        if(!messageShowVOS.isEmpty()) {
            messageShowVOS.forEach(message -> {
                Long sendUserId = message.getSendUserId();
                User user = userMapper.getUserById(sendUserId);
                message.setUserId(sendUserId);
                message.setUsername(user.getUsername());
                message.setImage(user.getImage());
            });
        }
        return messageShowVOS;
    }

    /**
     * 获取@我的消息
     * 
     * @param userId 用户ID
     * @return @消息列表
     */
    @Override
    public List<MessageShowVO> getAtMe(Long userId) {
        List<MessageShowVO> messageShowVOS = chatMessageMapper.getAtMe(userId);
        if(!messageShowVOS.isEmpty()) {
            messageShowVOS.forEach(message -> {
                Long sendUserId = message.getSendUserId();
                User user = userMapper.getUserById(sendUserId);
                message.setUserId(sendUserId);
                message.setUsername(user.getUsername());
                message.setImage(user.getImage());
            });
        }
        return messageShowVOS;
    }

    /**
     * 获取点赞我的消息
     * 
     * @param userId 用户ID
     * @return 点赞消息列表
     */
    @Override
    public List<MessageShowVO> getLikeMe(Long userId) {
        List<MessageShowVO> messageShowVOS = chatMessageMapper.getLikeMe(userId);
        if(!messageShowVOS.isEmpty()) {
            messageShowVOS.forEach(message -> {
                Long sendUserId = message.getSendUserId();
                User user = userMapper.getUserById(sendUserId);
                message.setUserId(sendUserId);
                message.setUsername(user.getUsername());
                message.setImage(user.getImage());
            });
        }
        return messageShowVOS;
    }

    /**
     * 获取系统消息
     * 
     * @param userId 用户ID
     * @return 系统消息列表
     */
    @Override
    public List<MessageShowVO> getSystemMessage(Long userId) {
        List<MessageShowVO> messageShowVOS = chatMessageMapper.getSystemMessage(userId);
        return messageShowVOS;
    }

    /**
     * 获取其他消息
     * 
     * @param userId 用户ID
     * @return 其他消息列表
     */
    @Override
    public List<MessageShowVO> getOtherMessage(Long userId) {
        List<MessageShowVO> messageShowVOS = chatMessageMapper.getOtherMessage(userId);
        if(!messageShowVOS.isEmpty()) {
            messageShowVOS.forEach(message -> {
                Long sendUserId = message.getSendUserId();
                User user = userMapper.getUserById(sendUserId);
                message.setUserId(sendUserId);
                message.setUsername(user.getUsername());
                message.setImage(user.getImage());
            });
        }
        return messageShowVOS;
    }

    /**
     * 合并消息列表
     * 
     * 功能：合并数据库消息和Redis离线消息
     * 业务逻辑：
     * 1. 使用LinkedHashMap保持插入顺序
     * 2. 数据库消息去重（按ID）
     * 3. Redis离线消息去重
     * 4. 过滤掉单方面删除的消息
     * 5. 按时间降序排列
     * 
     * @param dbMessages 数据库消息列表
     * @param redisMessages Redis离线消息列表
     * @return 合并后的消息列表
     */
    @Transactional
    public List<ChatMessage> mergeMessages(List<ChatMessage> dbMessages, List<Object> redisMessages){
        // 使用LinkedHashMap保持插入顺序
        Map<Long, ChatMessage> messageMap = new LinkedHashMap<>();

        // 数据库消息去重（按ID）
        dbMessages.forEach(msg -> messageMap.put(msg.getId(), msg));

        // Redis离线消息去重
        redisMessages.forEach(obj -> {
            ChatMessage msg = JSON.parseObject((String)obj, ChatMessage.class);
            messageMap.putIfAbsent(msg.getId(), msg);
        });

        // 遍历并过滤掉单方面删除掉的消息
        Iterator<Map.Entry<Long, ChatMessage>> iterator = messageMap.entrySet().iterator();
        while (iterator.hasNext()) {
            ChatMessage msg = iterator.next().getValue();
            boolean shouldRemove = false;
            // 判断发送方是否删除
            if (msg.getSendUserId().equals(CurrentHolder.getCurrent())) {
                shouldRemove = msg.getVisibilityBySender().equals(StatusConstant.STATUS_NO);
            }
            else if(msg.getReceiveUserId().equals(CurrentHolder.getCurrent())) {
                shouldRemove = msg.getVisibilityByReceiver().equals(StatusConstant.STATUS_NO);
            }

            if (shouldRemove) {
                iterator.remove();
            }
        }

        return new ArrayList<>(messageMap.values())
                .stream()
                .sorted((o1, o2) -> o2.getCreatedTime().compareTo(o1.getCreatedTime()))
                .collect(Collectors.toList());
    }
}

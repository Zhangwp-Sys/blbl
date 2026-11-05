package com.yilena.service.dao;

import com.yilena.service.entity.po.ChatMessage;
import com.yilena.service.entity.vo.MessageShowVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 聊天消息数据访问接口
 * 
 * 功能说明：
 * 1. 提供私信消息的增删改查
 * 2. 支持根据双方用户ID获取聊天记录
 * 3. 支持标记消息已读
 * 4. 支持获取"回复我"、"赞我"、系统消息等
 * 5. 支持删除半年前的过期消息
 * 
 */
@Mapper
public interface ChatMessageMapper {
    void addMessage(ChatMessage message);

    List<ChatMessage> getMessageByPair(Long sendUserId, Long receiveUserId);

    void markMessagesAsRead(Long receiveUserId, Long sendUserId);

    void updateStatusBatch(List<Long> messageIds);

    ChatMessage getMessageById(Long messageId);

    void deleteMessage(Long messageId);

    void updateMessageById(ChatMessage message);

    void updateExpiredStatus(Long receiveUserId, Long sendUserId);

    Long getMessageWhichUnReadAndIsExpired(Long receiveUserId, Long sendUserId);

    List<MessageShowVO> getAnswerMe(Long userId);

    List<MessageShowVO> getAtMe(Long userId);

    List<MessageShowVO> getLikeMe(Long userId);

    List<MessageShowVO> getSystemMessage(Long userId);

    List<MessageShowVO> getOtherMessage(Long userId);

    void deleteMessageHalfYearsAgo();
}

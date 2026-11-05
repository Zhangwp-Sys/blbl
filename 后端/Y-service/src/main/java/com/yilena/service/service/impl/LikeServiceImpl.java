package com.yilena.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.yilena.service.constant.*;
import com.yilena.service.dao.*;
import com.yilena.service.entity.po.*;
import com.yilena.service.entity.vo.PostVO;
import com.yilena.service.service.LikeService;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.SnowFlake;
import com.yilena.service.webSocket.ChatEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 点赞服务实现类
 * 
 * 功能说明：
 * 1. 实现点赞相关的业务逻辑
 * 2. 支持视频、动态、评论的点赞和取消点赞
 * 3. 实现点赞统计更新
 * 4. 发送点赞通知
 * 
 * 核心功能：
 * - 点赞操作：支持视频、动态、评论的点赞
 * - 取消点赞：支持视频、动态、评论的取消点赞
 * - 统计更新：实时更新被点赞实体的点赞数
 * - 通知功能：通过WebSocket发送点赞通知
 * 
 * 技术架构：
 * - 使用WebSocket实现实时通知
 * - 使用RabbitMQ同步动态数据到Elasticsearch
 * - 使用事务管理确保数据一致性
 * 
 */
@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService{

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * RabbitMQ消息模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 评论数据访问对象
     */
    private final CommentMapper commentMapper;

    /**
     * 动态数据访问对象
     */
    private final PostMapper postMapper;

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 聊天WebSocket端点
     */
    private final ChatEndpoint chatEndpoint;

    /**
     * 点赞数据访问对象
     */
    private final LikeMapper likeMapper;

    /**
     * 点赞
     * 
     * 功能：对视频、动态或评论进行点赞
     * 业务逻辑：
     * 1. 设置当前用户ID
     * 2. 根据实体类型更新对应实体的点赞数
     * 3. 保存点赞记录到数据库
     * 4. 发送点赞通知给被点赞者
     * 
     * @param like 点赞信息
     * @return void
     * @throws RuntimeException 当被点赞的视频、动态或评论不存在时抛出异常
     */
    @Transactional
    @Override
    public void doLike(Like like) {
        like.setUserId(CurrentHolder.getCurrent());

        ChatMessage chatMessage = new ChatMessage();

        Long receiveUserId = null;
        String username = userMapper.getUserById(like.getUserId()).getUsername();

        if(Objects.equals(like.getEntityType(), EntityConstant.VIDEO_TYPE)){
            Video video = videoMapper.getVideoById(like.getEntityId());
            if(video == null){
                throw new RuntimeException(StatusConstant.VIDEO_NOT_EXIST);
            }
            receiveUserId = video.getUserId();
            video.setLikes(video.getLikes()+1);
            videoMapper.updateVideo(video);

            chatMessage.setContent("用户" + username + "点赞了你的视频《" + video.getTitle() + "》");
        }
        else if(Objects.equals(like.getEntityType(), EntityConstant.POST_TYPE)){
            Post post = postMapper.getPostById(like.getEntityId());
            if(post == null){
                throw new RuntimeException(StatusConstant.POST_NOT_EXIST);
            }
            receiveUserId = post.getUserId();
            post.setLikes(post.getLikes()+1);
            postMapper.updatePost(post);

            chatMessage.setContent("用户" + username + "点赞了你的动态\n" + post.getContent());

            PostVO postVO = new PostVO();
            BeanUtils.copyProperties(post, postVO);
            postVO.setType(MqConstant.SEND_PUT);
            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.POST_BINDING_KEY, postVO);
        }
        else if(Objects.equals(like.getEntityType(), EntityConstant.COMMENT_TYPE)){
            Comment comment = commentMapper.getCommentById(like.getEntityId());
            if(comment == null){
                throw new RuntimeException(StatusConstant.COMMENT_NOT_EXIST);
            }
            receiveUserId = comment.getUserId();
            comment.setLikes(comment.getLikes()+1);
            commentMapper.updateComment(comment);

            chatMessage.setContent("用户" + username + "点赞了你的评论\n" + comment.getContent());
        }

        like.setId(snowFlake.getID());
        like.setCreatedTime(LocalDateTime.now());
        likeMapper.doLike(like);

        // 发送私信通知收到点赞
        chatMessage.setCreatedTime(LocalDateTime.now());
        chatMessage.setSendUserId(like.getUserId());
        chatMessage.setReceiveUserId(receiveUserId);
        chatMessage.setType(MessageTypeConstant.LIKE);
        chatMessage.setVisibilityBySender(StatusConstant.STATUS_NO);
        chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
        chatMessage.setId(snowFlake.getID());
        String json = JSON.toJSONString(chatMessage);
        chatEndpoint.sendMessage(json);
    }

    /**
     * 取消点赞
     * 
     * 功能：取消对视频、动态或评论的点赞
     * 业务逻辑：
     * 1. 设置当前用户ID
     * 2. 根据实体类型更新对应实体的点赞数（减1）
     * 3. 验证点赞数不能为负数
     * 4. 删除点赞记录
     * 
     * @param like 点赞信息
     * @return void
     * @throws RuntimeException 当实体不存在或点赞数已为0时抛出异常
     */
    @Transactional
    @Override
    public void undoLike(Like like) {
        like.setUserId(CurrentHolder.getCurrent());
        if(Objects.equals(like.getEntityType(), EntityConstant.VIDEO_TYPE)){
            Video video = videoMapper.getVideoById(like.getEntityId());
            if(video == null){
                throw new RuntimeException(StatusConstant.VIDEO_NOT_EXIST);
            }
            if(video.getLikes() == 0){
                throw new RuntimeException("点赞数不能为负数");
            }
            video.setLikes(video.getLikes()-1);
            videoMapper.updateVideo(video);
        }
        else if(Objects.equals(like.getEntityType(), EntityConstant.POST_TYPE)){
            Post post = postMapper.getPostById(like.getEntityId());
            if(post == null){
                throw new RuntimeException(StatusConstant.POST_NOT_EXIST);
            }
            if(post.getLikes() == 0){
                throw new RuntimeException("点赞数不能为负数");
            }
            post.setLikes(post.getLikes()-1);
            postMapper.updatePost(post);

            PostVO postVO = new PostVO();
            BeanUtils.copyProperties(post, postVO);
            postVO.setType(MqConstant.SEND_PUT);
            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.POST_BINDING_KEY, postVO);
        }
        else if(Objects.equals(like.getEntityType(), EntityConstant.COMMENT_TYPE)){
            Comment comment = commentMapper.getCommentById(like.getEntityId());
            if(comment == null){
                throw new RuntimeException(StatusConstant.COMMENT_NOT_EXIST);
            }
            comment.setLikes(comment.getLikes()-1);
            commentMapper.updateComment(comment);
        }

        likeMapper.undoLike(like);
    }

    /**
     * 检查是否已点赞
     * 
     * 功能：检查用户是否已点赞指定实体
     * 业务逻辑：
     * 1. 查询点赞记录
     * 2. 返回点赞信息（null表示未点赞）
     * 
     * @param like 点赞查询信息
     * @return 点赞信息（null表示未点赞）
     */
    @Override
    public Like isLike(Like like) {
        Like oldLike =  likeMapper.isLike(like);
        // 检测是否是之前视频的点赞
//        if(oldLike == null){
//            if(like.getEntityType().equals(EntityConstant.VIDEO_TYPE)){
//                Video video = videoMapper.getVideoById(like.getEntityId());
//                if(video != null){
//                    Long oldId = video.getLastId();
//                    oldLike =  likeMapper.isLike(like);
//                    // 如果存在点赞记录
//                    if(oldLike != null){
//                        // 删掉原来的记录
//                        Like dealLike = new Like();
//                        dealLike.setEntityId(oldId);
//                        dealLike.setEntityType(like.getEntityType());
//                        dealLike.setUserId(like.getUserId());
//                        likeMapper.undoLike(dealLike);
//
//                        // 增加新的记录
//                        dealLike.setId(snowFlake.getID());
//                        dealLike.setEntityId(video.getId());
//                        dealLike.setCreatedTime(oldLike.getCreatedTime());
//                        likeMapper.doLike(dealLike);
//                    }
//                }
//            }
//        }
        return oldLike;
    }
}

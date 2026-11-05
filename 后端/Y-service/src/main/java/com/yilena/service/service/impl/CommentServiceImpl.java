package com.yilena.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.houbb.sensitive.word.core.SensitiveWordHelper;
import com.yilena.service.constant.*;
import com.yilena.service.dao.CommentMapper;
import com.yilena.service.dao.PostMapper;
import com.yilena.service.dao.UserMapper;
import com.yilena.service.dao.VideoMapper;
import com.yilena.service.entity.dto.CommentQueryDTO;
import com.yilena.service.entity.po.*;
import com.yilena.service.entity.vo.CommentVO;
import com.yilena.service.entity.vo.PostVO;
import com.yilena.service.service.CommentService;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.SensitiveWordFilter;
import com.yilena.service.utils.SnowFlake;
import com.yilena.service.webSocket.ChatEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 评论服务实现类
 * 
 * 功能说明：
 * 1. 实现评论相关的业务逻辑
 * 2. 处理视频评论、动态评论、回复评论
 * 3. 实现评论通知功能
 * 4. 支持评论查询和排序
 * 
 * 核心功能：
 * - 评论发布：创建评论，过滤敏感词，更新统计
 * - 评论查询：支持热门、最新、点赞数排序
 * - 评论管理：更新、删除评论
 * - 通知功能：通过WebSocket发送评论通知
 * 
 * 技术架构：
 * - 使用WebSocket实现实时通知
 * - 使用敏感词过滤工具保障内容安全
 * - 使用事务管理确保数据一致性
 * - 支持多种实体类型的评论
 * 
 */
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    /**
     * 评论数据访问对象
     */
    private final CommentMapper commentMapper;

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * 动态数据访问对象
     */
    private final PostMapper postMapper;

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * RabbitMQ消息模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 聊天WebSocket端点
     */
    private final ChatEndpoint chatEndpoint;

    /**
     * 添加评论
     * 
     * 功能：创建新的评论
     * 业务逻辑：
     * 1. 设置评论ID、用户ID、时间等基本信息
     * 2. 过滤评论内容中的敏感词
     * 3. 保存评论到数据库
     * 4. 根据实体类型更新评论数
     * 5. 发送评论通知给被评论者
     * 6. 处理@用户的通知
     * 
     * @param comment 评论信息
     * @return void
     * @throws RuntimeException 当被评论的视频、动态、评论不存在或被@用户不存在时抛出异常
     */
    @Transactional
    @Override
    public void addComment(Comment comment) {
        comment.setId(snowFlake.getID());
        comment.setUserId(CurrentHolder.getCurrent());
        comment.setUpdatedTime(LocalDateTime.now());
        comment.setCreatedTime(LocalDateTime.now());
        comment.setComments(0);
        comment.setLikes(0);
        if(comment.getImageUrl().isEmpty()){
            comment.setImageUrl(null);
        }

        // 过滤敏感词
        comment.setContent(SensitiveWordFilter.filterSensitiveWords(comment.getContent()));

        commentMapper.addComment(comment);

        String username = userMapper.getUserById(comment.getUserId()).getUsername();

        Long receiveUserId =  null;
        String title = null;
        // 更改实体评论数
        if(Objects.equals(comment.getEntityType(), EntityConstant.VIDEO_TYPE)){
            Video video = videoMapper.getVideoById(comment.getEntityId());
            if(video == null){
                throw new RuntimeException(StatusConstant.VIDEO_NOT_EXIST);
            }
            receiveUserId = video.getUserId();
            title = video.getTitle();
            video.setComments(video.getComments() + 1);
            videoMapper.updateVideo(video);

            // 发送私信通知收到评论
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setCreatedTime(LocalDateTime.now());
            chatMessage.setSendUserId(comment.getUserId());
            chatMessage.setReceiveUserId(receiveUserId);
            chatMessage.setType(MessageTypeConstant.COMMENT);
            chatMessage.setVisibilityBySender(StatusConstant.STATUS_NO);
            chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
            chatMessage.setContent("用户" + username + "回复了你的视频《" +
                    title + "》\n" + comment.getContent());
            chatMessage.setId(snowFlake.getID());
            String json = JSON.toJSONString(chatMessage);
            chatEndpoint.sendMessage(json);
        }
        else if(Objects.equals(comment.getEntityType(),EntityConstant.POST_TYPE)){
            Post post = postMapper.getPostById(comment.getEntityId());
            if(post == null){
                throw new RuntimeException(StatusConstant.POST_NOT_EXIST);
            }
            receiveUserId = post.getUserId();
            title = post.getTitle();
            post.setComments(post.getComments() + 1);
            postMapper.updatePost(post);

            PostVO postVO = new PostVO();
            BeanUtils.copyProperties(post,postVO);
            postVO.setType(MqConstant.SEND_PUT);
            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE,MqConstant.POST_BINDING_KEY,postVO);

            // 发送私信通知收到评论
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setCreatedTime(LocalDateTime.now());
            chatMessage.setSendUserId(comment.getUserId());
            chatMessage.setReceiveUserId(receiveUserId);
            chatMessage.setType(MessageTypeConstant.COMMENT);
            chatMessage.setVisibilityBySender(StatusConstant.STATUS_NO);
            chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
            chatMessage.setContent("用户" + username + "回复了你的动态" + "\n" + comment.getContent());
            chatMessage.setId(snowFlake.getID());
            String json = JSON.toJSONString(chatMessage);
            chatEndpoint.sendMessage(json);
        }
        else if(Objects.equals(comment.getEntityType(),EntityConstant.COMMENT_TYPE)){
            Comment comment1 = commentMapper.getCommentById(comment.getEntityId());
            if(comment1 == null){
                throw new RuntimeException(StatusConstant.COMMENT_NOT_EXIST);
            }
            receiveUserId = comment1.getUserId();
            comment1.setComments(comment1.getComments() + 1);
            // 评论直接修改数据库即可
            commentMapper.updateComment(comment1);

            // 发送私信通知收到评论
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setCreatedTime(LocalDateTime.now());
            chatMessage.setSendUserId(comment.getUserId());
            chatMessage.setReceiveUserId(receiveUserId);
            chatMessage.setType(MessageTypeConstant.COMMENT);
            chatMessage.setVisibilityBySender(StatusConstant.STATUS_NO);
            chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
            chatMessage.setContent("用户" + username + "回复了你的评论\n" + comment.getContent());
            chatMessage.setId(snowFlake.getID());
            String json = JSON.toJSONString(chatMessage);
            chatEndpoint.sendMessage(json);
        }

        //检测评论里面是否有@
        if(comment.getContent().contains("@")){
            String[] split = comment.getContent().split("@");
            String AiUsername = split[1].split(" ")[0];
            User user = userMapper.getByUsername(AiUsername);
            if(user == null){
                throw new RuntimeException(UserConstant.USER_LOGIN_INEXIST_FAIL);
            }
            receiveUserId = user.getId();

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setCreatedTime(LocalDateTime.now());
            chatMessage.setSendUserId(comment.getUserId());
            chatMessage.setReceiveUserId(receiveUserId);
            chatMessage.setType(MessageTypeConstant.At);
            chatMessage.setVisibilityBySender(StatusConstant.STATUS_NO);
            chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
            chatMessage.setContent("用户" + username + "@了你");
            chatMessage.setId(snowFlake.getID());
            String json = JSON.toJSONString(chatMessage);
            chatEndpoint.sendMessage(json);
        }
    }

    /**
     * 获取评论列表
     * 
     * 功能：获取评论列表（支持热门、最新排序）
     * 业务逻辑：
     * 1. 根据排序类型查询评论列表
     * 2. 填充用户信息（头像、用户名）
     * 3. 返回评论列表
     * 
     * @param comment 评论查询条件
     * @return 评论列表
     */
    @Override
    public List<CommentVO> getCommentByList(Comment comment) {
        List<CommentVO>commentVOS = null;
        if(comment.getType().equals(EntityConstant.HOT_TYPE)){
            commentVOS = commentMapper.getCommentByHot(comment);
        }else if(comment.getType().equals(EntityConstant.NEW_TYPE)){
            commentVOS = commentMapper.getCommentByNew(comment);
        }
        if(commentVOS == null){
            return null;
        }
        for (CommentVO commentVO : commentVOS){
            Long userId = commentVO.getUserId();
            User user = userMapper.getUserById(userId);
            commentVO.setImage(user.getImage());
            commentVO.setUsername(user.getUsername());
        }
        return commentVOS;
    }

    /**
     * 更新评论
     * 
     * 功能：更新评论内容
     * 业务逻辑：
     * 1. 验证评论是否存在
     * 2. 更新评论内容和图片
     * 3. 设置更新时间
     * 4. 保存到数据库
     * 
     * @param comment 评论更新信息
     * @return void
     * @throws RuntimeException 当评论不存在时抛出异常（COMMENT_NOT_EXIST）
     */
    @Override
    public void updateComment(Comment comment) {
        Comment oldComment = commentMapper.getCommentById(comment.getId());
        if(oldComment == null){
            throw new RuntimeException(StatusConstant.COMMENT_NOT_EXIST);
        }
        oldComment.setContent(comment.getContent());
        oldComment.setImageUrl(comment.getImageUrl());
        oldComment.setUpdatedTime(LocalDateTime.now());
        commentMapper.updateComment(oldComment);
    }

    /**
     * 删除评论
     * 
     * 功能：批量删除评论
     * 业务逻辑：
     * 1. 根据ID列表批量查询评论
     * 2. 验证评论对应的视频是否存在，更新视频评论数（-1）
     * 3. 批量删除评论
     * 
     * @param ids 评论ID列表
     * @return void
     * @throws RuntimeException 当评论对应的视频不存在时抛出异常（VIDEO_NOT_EXIST）
     */
    @Transactional
    @Override
    public void deleteComment(List<Long> ids) {
        List<Comment> comments = commentMapper.getCommentByIds(ids);

        comments.forEach(comment -> {Video video = videoMapper.getVideoById(comment.getEntityId());
            if(video == null){
                throw new RuntimeException(StatusConstant.VIDEO_NOT_EXIST);
            }
            video.setComments(video.getComments() - 1);
            videoMapper.updateVideo(video);}
        );

        commentMapper.deleteCommentByIds(ids);
    }

    /**
     * 查询评论
     * 
     * 功能：根据条件查询评论（支持多种排序）
     * 业务逻辑：
     * 1. 根据用户ID获取视频ID列表
     * 2. 根据排序类型查询评论
     * 3. 填充视频标题和用户信息
     * 4. 返回评论列表
     * 
     * @param commentQueryDTO 查询条件
     * @return 评论列表
     */
    @Transactional
    @Override
    public List<Comment> getCommentQuery(CommentQueryDTO commentQueryDTO) {
        Long currentUserId = CurrentHolder.getCurrent();
        List<Long> videoIds = videoMapper.getVideoIdByUserId(currentUserId,commentQueryDTO.getVideoTitle());
        // 如果没有结果，则返回空列表
        if(videoIds == null){
            return List.of();
        }

        List<Comment> comments = null;
        if(commentQueryDTO.getSortType().equals(EntityConstant.COMMENT_NEW_TYPE)){
            comments =  commentMapper.getCommentsQueryByNewSortType(videoIds);
        }else if(commentQueryDTO.getSortType().equals(EntityConstant.COMMENT_LIKE_TYPE)){
            comments =  commentMapper.getCommentsQueryByLikeSortType(videoIds);
        }else if(commentQueryDTO.getSortType().equals(EntityConstant.COMMENT_MOST_COMMENTS_TYPE)){
            comments =  commentMapper.getCommentsQueryByMostCommentsSortType(videoIds);
        }

        // 如果没有结果，则返回空列表
        if(comments == null){
            return List.of();
        }
        comments.forEach(comment -> {
            Video video = videoMapper.getVideoById(comment.getEntityId());
            if(video != null){
                comment.setVideoTitle(video.getTitle());
            }
            User user = userMapper.getUserById(comment.getUserId());
            if(user != null){
                comment.setUsername(user.getUsername());
            }
        });
        return comments;
    }
}

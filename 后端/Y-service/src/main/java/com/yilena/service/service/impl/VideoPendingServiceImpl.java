package com.yilena.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yilena.service.constant.MessageTypeConstant;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.dao.*;
import com.yilena.service.entity.PageResult;
import com.yilena.service.entity.dto.VideoPendingDTO;
import com.yilena.service.entity.dto.VideoPendingStatusDTO;
import com.yilena.service.entity.po.*;
import com.yilena.service.entity.vo.VideoCollectionVO;
import com.yilena.service.entity.vo.VideoVO;
import com.yilena.service.utils.SnowFlake;
import com.yilena.service.utils.VideoDurationUtils;
import com.yilena.service.webSocket.ChatEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import com.yilena.service.service.VideoPendingService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 视频待审核服务实现类
 * 
 * 功能说明：
 * 1. 实现视频审核相关的业务逻辑
 * 2. 处理视频审核通过/拒绝操作
 * 3. 管理待审核视频的生命周期
 * 
 * 核心功能：
 * - 分页查询：查询待审核/已审核视频列表
 * - 审核操作：审核通过/拒绝视频
 * - 视频发布：审核通过后发布到视频表
 * - 消息通知：通过WebSocket发送审核结果通知
 * 
 * 技术特点：
 * - 使用RabbitMQ异步更新ES索引和视频合集
 * - 使用WebSocket实时推送审核结果
 * - 事务管理确保数据一致性
 * 
 */
@Service
@RequiredArgsConstructor
public class VideoPendingServiceImpl implements VideoPendingService{

    /**
     * 待审核视频数据访问对象
     */
    private final VideoPendingMapper videoPendingMapper;

    /**
     * RabbitMQ消息模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * WebSocket聊天端点
     */
    private final ChatEndpoint chatEndpoint;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 视频合集数据访问对象
     */
    private final VideoCollectionMapper videoCollectionMapper;

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 关注数据访问对象
     */
    private final FollowMapper followMapper;

    /**
     * 分页查询待审核视频
     * 
     * 功能：分页查询待审核或已审核的视频列表
     * 业务逻辑：
     * 1. 根据状态选择查询待审核或已审核视频
     * 2. 解析标签JSON数据
     * 3. 返回分页结果
     * 
     * @param videoPendingDTO 查询条件
     * @return 视频分页结果
     */
    @Override
    public PageResult<VideoPending> getVideoPendingByPage(VideoPendingDTO videoPendingDTO) {
        PageHelper.startPage(videoPendingDTO.getPage(),videoPendingDTO.getPageSize());
        List<VideoPending> videoPendings = null;
        if(!Objects.equals(videoPendingDTO.getStatus(), StatusConstant.STATUS_YES)) {
             videoPendings = videoPendingMapper.getVideoPendingByPage(videoPendingDTO);
        }else{
            videoPendings = videoMapper.getVideoByPage(videoPendingDTO);
        }
        videoPendings.forEach(videoPending -> {
            String tgsJson = videoPending.getTagsJson();
            List<String> tags = JSON.parseArray(tgsJson, String.class);
            videoPending.setTags(tags);
        });
        Page<VideoPending> p = (Page<VideoPending>)videoPendings;
        return new PageResult<>(p.getTotal(),p.getResult());
    }

    /**
     * 根据ID获取待审核视频详情
     * 
     * @param id 视频ID
     * @return 视频详情
     */
    @Override
    public VideoPending getVideoPendingById(Long id) {
        VideoPending videoPending = videoPendingMapper.getVideoPendingById(id);
        String tgsJson = videoPending.getTagsJson();
        List<String> tags = JSON.parseArray(tgsJson, String.class);
        videoPending.setTags(tags);

        VideoCollection videoCollection = videoCollectionMapper.getVideoCollectionById(videoPending.getCollectionId());
        videoPending.setCollectionName(videoCollection.getTitle());
        return videoPending;
    }

    /**
     * 更新视频审核状态
     * 
     * 功能：审核视频并处理审核结果
     * 业务逻辑：
     * 1. 查询待审核视频
     * 2. 更新审核状态
     * 3. 如果审核通过：
     *    - 添加到视频表
     *    - 更新用户视频计数
     *    - 发送MQ消息更新ES索引
     *    - 发送MQ消息添加到视频合集
     *    - 通过WebSocket通知关注者
     * 4. 如果审核拒绝：
     *    - 发送私信通知用户
     * 
     * @param videoPendingStatusDTO 审核状态更新信息
     * @throws RuntimeException 视频不存在时抛出异常
     */
    @Transactional
    @Override
    public void updateVideoPendingStatus(VideoPendingStatusDTO videoPendingStatusDTO) {
        VideoPending videoPending = videoPendingMapper.getVideoPendingById(videoPendingStatusDTO.getId());
        videoPending.setStatus(videoPendingStatusDTO.getStatus());
        if(videoPendingStatusDTO.getReason() != null){
            videoPending.setReason(videoPendingStatusDTO.getReason());
        }
        videoPendingMapper.updateVideoPending(videoPending);

        if(Objects.equals(videoPending.getStatus(), StatusConstant.STATUS_YES)) {
            // 添加到视频表
            Video video = new Video();
            BeanUtils.copyProperties(videoPending, video);

            User user = userMapper.getUserById(videoPending.getUserId());
            user.setVideoCount(user.getVideoCount() + 1);
            userMapper.updateUserById(user);

            if(videoPending.getLastId() != null) {
                // 保持新旧id一致
                video.setId(videoPending.getLastId());
            }else{
                video.setId(snowFlake.getID());
            }
            videoMapper.addVideo(video);

            // 删除待审核表
            videoPendingMapper.deleteVideoPending(videoPending.getId());

            // 发送消息添加到es索引库
            VideoVO videoVO = new VideoVO();
            videoVO.setCreatedTime(video.getCreatedTime());
            BeanUtils.copyProperties(video, videoVO);
            videoVO.setType(MqConstant.SEND_ADD);
            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.VIDEO_BINDING_KEY, videoVO);

            // 发送消息添加到投稿合集
            VideoCollectionVO videoCollectionVO = new VideoCollectionVO();
            videoCollectionVO.setId(video.getCollectionId());
            videoCollectionVO.setVideoIdsJson("[" + video.getId().toString() + "]");
            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.COLLECTION_BINDING_KEY, videoCollectionVO);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType(MessageTypeConstant.VIDEO);
            String videoJson = JSON.toJSONString(video);
            chatMessage.setContent(videoJson);
            chatMessage.setVisibilityBySender(StatusConstant.STATUS_YES);
            chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
            chatMessage.setSendUserId(video.getUserId());
            chatMessage.setCreatedTime(LocalDateTime.now());
            chatMessage.setStatus(StatusConstant.STATUS_YES);

            List<Long> receiveUserIds = followMapper.getFollowedIds(video.getUserId());
            chatMessage.setReceiveUserIds(receiveUserIds);

            String json = JSON.toJSONString(chatMessage);
            chatEndpoint.sendMessage(json);
        }else if(Objects.equals(videoPending.getStatus(), StatusConstant.STATUS_NO)){

            // 发送私信通知
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setCreatedTime(LocalDateTime.now());
            // 官方默认发送人id为0
            chatMessage.setSendUserId(0L);
            chatMessage.setReceiveUserId(videoPending.getUserId());
            chatMessage.setType(MessageTypeConstant.MANAGER);
            chatMessage.setVisibilityBySender(StatusConstant.STATUS_YES);
            chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
            chatMessage.setContent("您的视频《"+videoPending.getTitle()+"》未通过审核，" +
                    "请修改后再次提交。\n" + "原因：" + videoPendingStatusDTO.getReason());
            chatMessage.setId(snowFlake.getID());
            String json = JSON.toJSONString(chatMessage);
            chatEndpoint.sendMessage(json);
        }
    }

    /**
     * 视频下架迁移到审核表
     * 
     * 功能：将已发布的视频下架并迁移到审核表
     * 业务逻辑：
     * 1. 查询视频信息
     * 2. 迁移到审核表（设置状态为待审核）
     * 3. 删除原视频记录
     * 4. 发送私信通知用户
     * 
     * @param videoPendingStatusDTO 视频下架信息
     * @throws RuntimeException 视频不存在时抛出异常
     */
    @Transactional
    @Override
    public void updateVideoPendingFromVideo(VideoPendingStatusDTO videoPendingStatusDTO) {
        Video video = videoMapper.getVideoById(videoPendingStatusDTO.getId());
        VideoPending videoPending = new VideoPending();
        BeanUtils.copyProperties(video,videoPending);

        // 迁移到审核表
        videoPending.setStatus(StatusConstant.STATUS_NO);
        videoPending.setReason(videoPendingStatusDTO.getReason());
        videoPending.setLastId(video.getId());
        video.setId(snowFlake.getID());
        videoPendingMapper.addVideoPendingWhichNoPass(videoPending);
        videoMapper.deleteVideo(videoPendingStatusDTO.getId());

        // 发送私信通知
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setCreatedTime(LocalDateTime.now());
        // 官方默认发送人id为0
        chatMessage.setSendUserId(0L);
        chatMessage.setReceiveUserId(video.getUserId());
        chatMessage.setType(MessageTypeConstant.MANAGER);
        chatMessage.setVisibilityBySender(StatusConstant.STATUS_YES);
        chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
        chatMessage.setContent("您的视频《"+video.getTitle()+"》因违规而被下架，" +
                "请修改后再次提交。\n" + "原因：" + videoPendingStatusDTO.getReason());
        chatMessage.setId(snowFlake.getID());
        String json = JSON.toJSONString(chatMessage);
        chatEndpoint.sendMessage(json);
    }

    /**
     * 根据用户ID和状态获取视频列表
     * 
     * @param userId 用户ID
     * @param status 视频状态
     * @return 视频列表
     */
    @Override
    public List<VideoPending> getVideoPendingByStatus(Long userId, Integer status) {
        if(!Objects.equals(status, StatusConstant.STATUS_YES)) {
            return videoPendingMapper.getVideoPendingByStatus(userId, status);
        }else{
            return videoMapper.getAllVideoByUserId(userId);
        }
    }

    /**
     * 更新待审核视频信息
     * 
     * 功能：更新视频信息，如果视频URL变更则重新计算时长
     * 
     * @param videoPending 视频更新信息
     */
    @Override
    public void updateVideoPending(VideoPending videoPending) {
        if(videoPending.getVideoUrl() != null){
            // 获取视频时长
            long time = VideoDurationUtils.getVideoDuration(videoPending.getVideoUrl());
            videoPending.setTime(time);
        }
        videoPendingMapper.updateVideoPending(videoPending);
    }

    /**
     * 删除待审核视频
     * 
     * @param id 视频ID
     */
    @Override
    public void deleteVideoPending(Long id) {
        videoPendingMapper.deleteVideoPending(id);
    }

    /**
     * 重新上传视频
     * 
     * 功能：将视频状态重置为待审核
     * 
     * @param id 视频ID
     */
    @Override
    public void reuploadVideoPending(Long id) {
        VideoPending videoPending = videoPendingMapper.getVideoPendingById(id);
        videoPending.setStatus(StatusConstant.STATUS_WAIT);
        videoPendingMapper.updateVideoPending(videoPending);
    }
}

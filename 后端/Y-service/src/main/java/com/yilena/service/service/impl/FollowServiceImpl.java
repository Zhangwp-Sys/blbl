package com.yilena.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yilena.service.constant.*;
import com.yilena.service.dao.FollowMapper;
import com.yilena.service.dao.UserMapper;
import com.yilena.service.entity.PageResult;
import com.yilena.service.entity.dto.FollowDTO;
import com.yilena.service.entity.dto.FollowPageDTO;
import com.yilena.service.entity.po.ChatMessage;
import com.yilena.service.entity.po.UserFollowing;
import com.yilena.service.entity.vo.UserVO;
import com.yilena.service.service.FollowService;
import com.yilena.service.utils.SnowFlake;
import com.yilena.service.webSocket.ChatEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 关注服务实现类
 * 
 * 功能说明：
 * 1. 实现用户关注相关的业务逻辑
 * 2. 处理关注、取消关注、特别关注功能
 * 3. 实现关注关系查询
 * 4. 发送关注通知
 * 
 * 核心功能：
 * - 关注管理：关注、取消关注、特别关注
 * - 关注查询：分页查询关注列表、检查关注状态
 * - 通知功能：通过WebSocket发送关注通知
 * - 数据同步：使用RabbitMQ同步关注数据到Elasticsearch
 * 
 * 技术架构：
 * - 使用WebSocket实现实时通知
 * - 使用RabbitMQ异步同步关注数据
 * - 使用事务管理确保数据一致性
 * - 使用PageHelper实现分页查询
 * 
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    /**
     * 关注数据访问对象
     */
    private final FollowMapper followMapper;

    /**
     * RabbitMQ消息模板
     */
    private final RabbitTemplate rabbitTemplate;

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
     * 关注用户
     * 
     * 功能：关注指定用户
     * 业务逻辑：
     * 1. 验证不能关注自己
     * 2. 普通关注：检查是否已关注，添加关注关系
     * 3. 特别关注：检查是否已关注，设置特别关注标记
     * 4. 发送关注通知给被关注者
     * 
     * @param followDTO 关注信息
     * @throws RuntimeException 关注失败时抛出异常
     */
    @Transactional
    @Override
    public void follow(FollowDTO followDTO) {
        if(Objects.equals(followDTO.getFollowedId(), followDTO.getFollowerId())){
            throw new RuntimeException(FollowConstant.FOLLOW_SAME_USER_FAIL);
        }
        UserFollowing userFollowing = new UserFollowing();
        BeanUtils.copyProperties(followDTO, userFollowing);
        // 判断是否为特别关注
        if(Objects.equals(followDTO.getIsSpecial(), FollowConstant.NotSpecial)){
            log.info("用户:{}关注用户:{}", followDTO.getFollowerId(), followDTO.getFollowedId());
            if(followMapper.getById(followDTO.getFollowerId(), followDTO.getFollowedId()) != null){
                throw new RuntimeException(FollowConstant.FOLLOW_ADD_FAIL);
            }
            userFollowing.setUpdatedTime(LocalDateTime.now());
            userFollowing.setCreatedTime(LocalDateTime.now());
            userFollowing.setId(snowFlake.getID());
            followMapper.addFollow(userFollowing);

            userFollowing.setType(MqConstant.SEND_ADD);
            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.FOLLOW_BINDING_KEY, userFollowing);
        }
        else if(Objects.equals(followDTO.getIsSpecial(), FollowConstant.Special)){
            log.info("用户:{}特别关注用户:{}", followDTO.getFollowerId(), followDTO.getFollowedId());
            if(followMapper.getById(followDTO.getFollowerId(), followDTO.getFollowedId()) == null){
                throw new RuntimeException(FollowConstant.FOLLOW_DELETE_FAIL);
            }
            userFollowing.setUpdatedTime(LocalDateTime.now());
            followMapper.updateSpecialFollow(userFollowing);
        }

        String username = userMapper.getUserById(followDTO.getFollowerId()).getUsername();

        // 发送私信通知受到关注
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setCreatedTime(LocalDateTime.now());
        chatMessage.setSendUserId(followDTO.getFollowerId());
        chatMessage.setReceiveUserId(followDTO.getFollowedId());
        chatMessage.setType(MessageTypeConstant.FOLLOW);
        chatMessage.setVisibilityBySender(StatusConstant.STATUS_NO);
        chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
        chatMessage.setContent("用户" + username + "关注了你");
        chatMessage.setId(snowFlake.getID());
        String json = JSON.toJSONString(chatMessage);
        chatEndpoint.sendMessage(json);
    }

    /**
     * 取消关注
     * 
     * 功能：取消关注指定用户
     * 业务逻辑：
     * 1. 验证不能取消关注自己
     * 2. 普通取消关注：检查是否已关注，删除关注关系
     * 3. 取消特别关注：检查是否已关注，取消特别关注标记
     * 
     * @param followDTO 关注信息
     * @throws RuntimeException 取消关注失败时抛出异常
     */
    @Transactional
    @Override
    public void unfollow(FollowDTO followDTO) {
        if(Objects.equals(followDTO.getFollowedId(), followDTO.getFollowerId())){
            throw new RuntimeException(FollowConstant.FOLLOW_SAME_USER_FAIL);
        }
        UserFollowing userFollowing = new UserFollowing();
        BeanUtils.copyProperties(followDTO, userFollowing);
        // 判断是否为特别关注
        if(Objects.equals(followDTO.getIsSpecial(), FollowConstant.NotSpecial)){
            log.info("用户:{}取消关注用户:{}", followDTO.getFollowerId(), followDTO.getFollowedId());
            if(followMapper.getById(followDTO.getFollowerId(), followDTO.getFollowedId()) == null){
                throw new RuntimeException(FollowConstant.FOLLOW_DELETE_FAIL);
            }

            userFollowing.setUpdatedTime(LocalDateTime.now());
            followMapper.deleteFollow(userFollowing);

            userFollowing.setType(MqConstant.SEND_DELETE);
            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.FOLLOW_BINDING_KEY, userFollowing);
        }
        else if(Objects.equals(followDTO.getIsSpecial(), FollowConstant.Special)){
            log.info("用户:{}取消特别关注用户:{}", followDTO.getFollowerId(), followDTO.getFollowedId());
            userFollowing.setUpdatedTime(LocalDateTime.now());
            userFollowing.setIsSpecial(FollowConstant.NotSpecial);
            followMapper.updateSpecialFollow(userFollowing);
        }
    }

    /**
     * 分页查询关注列表
     * 
     * 功能：分页查询用户的关注列表
     * 业务逻辑：
     * 1. 获取用户关注的用户ID列表
     * 2. 根据用户名模糊查询
     * 3. 分页返回用户信息
     * 
     * @param followPageDTO 分页查询条件
     * @return 关注用户分页列表
     */
    @Override
    public PageResult<UserVO> getFollowByPage(FollowPageDTO followPageDTO) {
        List<Long> followingIds = followMapper.getFollowingIds(followPageDTO.getId());
        if(followingIds.isEmpty()){
            return new PageResult<>(0L, null);
        }
        PageHelper.startPage(followPageDTO.getPage(), followPageDTO.getPageSize());
        List<UserVO> users = userMapper.getUserByIds(followingIds,followPageDTO.getUsername());
        Page<UserVO> p = (Page<UserVO>) users;
        return new PageResult<>(p.getTotal(), p.getResult());
    }

    /**
     * 检查是否已关注
     * 
     * 功能：检查用户是否已关注指定作者
     * 业务逻辑：
     * 1. 查询关注关系
     * 2. 返回关注信息（null表示未关注）
     * 
     * @param userId 用户ID
     * @param authorId 作者ID
     * @return 关注信息（null表示未关注）
     */
    @Override
    public UserFollowing isFollow(Long userId, Long authorId) {
        return followMapper.isFollow(userId, authorId);
    }
}

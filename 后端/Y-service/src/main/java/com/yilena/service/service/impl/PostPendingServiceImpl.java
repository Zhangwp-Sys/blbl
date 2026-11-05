package com.yilena.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yilena.service.constant.MessageTypeConstant;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.dao.PostMapper;
import com.yilena.service.entity.PageResult;
import com.yilena.service.entity.dto.PostPendingDTO;
import com.yilena.service.entity.dto.PostPendingPageQueryDTO;
import com.yilena.service.entity.po.ChatMessage;
import com.yilena.service.entity.po.Post;
import com.yilena.service.entity.vo.PostVO;
import com.yilena.service.service.PostPendingService;
import com.yilena.service.utils.SnowFlake;
import com.yilena.service.webSocket.ChatEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态待审核服务实现类
 * 
 * 功能说明：
 * 1. 实现动态审核相关的业务逻辑
 * 2. 处理动态违规设为私密操作
 * 3. 管理待审核动态的分页查询
 * 
 * 核心功能：
 * - 审核操作：将违规动态设为私密
 * - 分页查询：查询待审核动态列表
 * - 消息通知：通过WebSocket发送审核结果通知
 * 
 */
@Service
@RequiredArgsConstructor
public class PostPendingServiceImpl implements PostPendingService {

    /**
     * 动态数据访问对象
     */
    private final PostMapper postMapper;

    /**
     * WebSocket聊天端点
     */
    private final ChatEndpoint chatEndpoint;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 更新动态审核状态
     * 
     * 功能：将违规动态设为私密
     * 业务逻辑：
     * 1. 验证动态是否存在
     * 2. 设置动态为私密状态
     * 3. 发送私信通知用户
     * 
     * @param postPendingDTO 审核信息
     * @throws RuntimeException 动态不存在时抛出异常
     */
    @Transactional
    @Override
    public void updatePostPendingStatus(PostPendingDTO postPendingDTO) {
        Post post = postMapper.getPostById(postPendingDTO.getId());
        if (post == null) {
            throw new RuntimeException(StatusConstant.POST_NOT_EXIST);
        }
        post.setVisibility(StatusConstant.STATUS_NO);
        postMapper.updatePost(post);

        // 发送私信通知
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setCreatedTime(LocalDateTime.now());
        // 官方默认发送人id为0
        chatMessage.setSendUserId(0L);
        chatMessage.setReceiveUserId(post.getUserId());
        chatMessage.setType(MessageTypeConstant.MANAGER);
        chatMessage.setVisibilityBySender(StatusConstant.STATUS_YES);
        chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
        chatMessage.setContent("您的动态《"+post.getTitle()+"》因违规而被设为私密，" +
                "请修改后再发布。\n" + "原因：" + postPendingDTO.getReason());
        chatMessage.setId(snowFlake.getID());
        String json = JSON.toJSONString(chatMessage);
        chatEndpoint.sendMessage(json);
    }

    /**
     * 分页查询待审核动态
     * 
     * 功能：分页查询违规动态列表
     * 业务逻辑：
     * 1. 设置分页参数
     * 2. 查询动态列表
     * 3. 解析标签和图片URL JSON数据
     * 4. 返回分页结果
     * 
     * @param postPendingPageQueryDTO 分页查询条件
     * @return 动态分页结果
     */
    @Override
    public PageResult<PostVO> getPostPendingByPage(PostPendingPageQueryDTO postPendingPageQueryDTO) {
        PageHelper.startPage(postPendingPageQueryDTO.getPage(), postPendingPageQueryDTO.getPageSize());
        List<PostVO> posts = postMapper.getPostPendingByPage(postPendingPageQueryDTO);
        posts.forEach(postVO -> {
            String tgsJson = postVO.getTagsJson();
            List<String> tags = JSON.parseArray(tgsJson, String.class);
            postVO.setTags(tags);

            String imageUrlJson = postVO.getImageUrlJson();
            List<String> imageUrl = JSON.parseArray(imageUrlJson, String.class);
            postVO.setImageUrl(imageUrl);
        });
        Page<PostVO> p = (Page<PostVO>) posts;
        return new PageResult<>(p.getTotal(), p.getResult());
    }
}

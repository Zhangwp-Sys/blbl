package com.yilena.service.service.impl;

import com.yilena.service.constant.EntityConstant;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.dao.PostMapper;
import com.yilena.service.dao.ShareMapper;
import com.yilena.service.dao.VideoMapper;
import com.yilena.service.entity.po.Post;
import com.yilena.service.entity.po.Share;
import com.yilena.service.entity.po.Video;
import com.yilena.service.service.ShareService;
import com.yilena.service.utils.SnowFlake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 分享服务实现类
 * 
 * 功能说明：
 * 1. 实现视频和动态的分享功能
 * 2. 检查是否已分享
 * 3. 执行分享操作并更新分享计数
 * 
 * 核心功能：
 * - 分享检查：检查用户是否已分享指定实体
 * - 分享操作：支持视频和动态的分享
 * - 统计更新：更新被分享实体的分享计数
 * 
 */
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements ShareService {

    /**
     * 分享数据访问对象
     */
    private final ShareMapper shareMapper;

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * 动态数据访问对象
     */
    private final PostMapper postMapper;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 检查是否已分享
     * 
     * @param share 分享查询信息
     * @return 分享记录（null表示未分享）
     */
    @Transactional
    @Override
    public Share getIsShare(Share share) {
        return shareMapper.getIsShare(share);
    }

    /**
     * 执行分享操作
     * 
     * 功能：分享视频或动态
     * 业务逻辑：
     * 1. 根据实体类型查询实体
     * 2. 验证实体是否存在
     * 3. 保存分享记录
     * 4. 更新实体分享计数
     * 
     * @param share 分享信息
     */
    public void doShare(Share share) {
        if(share.getEntityType().equals(EntityConstant.VIDEO_TYPE)){
            Video video = videoMapper.getVideoById(share.getEntityId());
            if(video == null){
                throw new RuntimeException(StatusConstant.VIDEO_NOT_EXIST);
            }
            share.setId(snowFlake.getID());
            share.setUserId(video.getUserId());
            share.setEntityType(EntityConstant.VIDEO_TYPE);
            share.setEntityId(video.getId());
            share.setCreatedTime(LocalDateTime.now());

            shareMapper.addShare(share);

            video.setShares(video.getShares()+1);
            videoMapper.updateVideo(video);
        }else if(share.getEntityType().equals(EntityConstant.POST_TYPE)){
            Post post = postMapper.getPostById(share.getEntityId());
            if(post == null){
                throw new RuntimeException(StatusConstant.POST_NOT_EXIST);
            }
            share.setId(snowFlake.getID());
            share.setUserId(post.getUserId());
            share.setEntityType(EntityConstant.POST_TYPE);
            share.setEntityId(post.getId());
            share.setCreatedTime(LocalDateTime.now());

            shareMapper.addShare(share);

            post.setShares(post.getShares()+1);
            postMapper.updatePost(post);
        }
    }
}

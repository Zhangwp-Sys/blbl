package com.yilena.service.service.impl;

import com.yilena.service.constant.EntityConstant;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.dao.CoinsMapper;
import com.yilena.service.dao.UserMapper;
import com.yilena.service.dao.VideoMapper;
import com.yilena.service.entity.po.Video;
import com.yilena.service.entity.po.VideoCoins;
import com.yilena.service.entity.vo.VideoVO;
import com.yilena.service.service.CoinsService;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.SnowFlake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投币服务实现类
 * 
 * 功能说明：
 * 1. 实现视频投币相关的业务逻辑
 * 2. 处理投币操作和投币记录
 * 3. 实现投币查询功能
 * 
 * 核心功能：
 * - 投币操作：为视频投币，更新视频投币数
 * - 投币查询：查询用户投过的视频列表
 * - 投币检查：检查用户是否已为视频投币
 * 
 */
@Service
@RequiredArgsConstructor
public class CoinsServiceImpl implements CoinsService {

    /**
     * 投币数据访问对象
     */
    private final CoinsMapper coinsMapper;

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 添加投币
     * 
     * 功能：为视频投币
     * 业务逻辑：
     * 1. 设置当前用户ID
     * 2. 更新视频投币数
     * 3. 保存投币记录到数据库
     * 
     * @param videoCoins 投币信息
     */
    @Transactional
    @Override
    public void addCoins(VideoCoins videoCoins) {
        videoCoins.setUserId(CurrentHolder.getCurrent());
        Long videoId = videoCoins.getVideoId();
        Video video = videoMapper.getVideoById(videoId);
        video.setCoins(video.getCoins()+videoCoins.getCoinsCount());
        videoMapper.updateVideo(video);

        videoCoins.setCreatedTime(LocalDateTime.now());
        videoCoins.setId(snowFlake.getID());
        coinsMapper.addCoins(videoCoins);
    }

    /**
     * 获取用户投币视频列表
     * 
     * 功能：获取用户投过币的视频列表
     * 业务逻辑：
     * 1. 根据用户ID查询投币的视频ID列表
     * 2. 查询视频详细信息
     * 3. 填充用户名信息
     * 4. 返回视频列表
     * 
     * @param userId 用户ID
     * @return 投币视频列表
     */
    @Override
    public List<VideoVO> getVideoCoins(Long userId) {
        List<Long> videosIds = coinsMapper.getVideoCoins(userId);
        if (videosIds.isEmpty()){
            throw new RuntimeException("没有投币的视频");
        }
        List<VideoVO> videoVOS = videoMapper.getVideoCoins(videosIds);
        if(videoVOS.isEmpty()){
            throw new RuntimeException(StatusConstant.VIDEO_NOT_EXIST);
        }
        videoVOS.forEach(videoVO -> {
            Long videoUserId = videoVO.getUserId();
            videoVO.setUsername(userMapper.getUserById(videoUserId).getUsername());
        });
        return videoVOS;
    }

    /**
     * 检查是否已投币
     * 
     * 功能：检查用户是否已为视频投币
     * 业务逻辑：
     * 1. 查询投币记录
     * 2. 返回投币信息（null表示未投币）
     * 
     * @param videoId 视频ID
     * @param userId 用户ID
     * @return 投币信息（null表示未投币）
     */
    @Override
    public VideoCoins isCoins(Long videoId, Long userId) {
        VideoCoins videoCoins =  coinsMapper.isCoins(videoId, userId);
//        // 检测是否是之前视频的投币
//        if(videoCoins == null){
//                Video video = videoMapper.getVideoById(videoId);
//                if(video != null){
//                    Long oldId = video.getLastId();
//                    Integer oldCoins = video.getCoins();
//                    videoCoins = coinsMapper.isCoins(oldId, userId);
//                    // 如果存在投币记录
//                    if(videoCoins != null){
//                        // 删除旧纪录
//                        VideoCoins videoCoins1 = new VideoCoins();
//                        videoCoins1.setVideoId(oldId);
//                        videoCoins1.setUserId(userId);
//                        coinsMapper.deleteCoins(videoCoins1);
//
//                        // 添加新记录
//                        videoCoins1.setId(snowFlake.getID());
//                        videoCoins1.setVideoId(videoId);
//                        videoCoins1.setCreatedTime(videoCoins.getCreatedTime());
//                        coinsMapper.addCoins(videoCoins1);
//                    }
//                }
//        }
        return videoCoins;
    }
}

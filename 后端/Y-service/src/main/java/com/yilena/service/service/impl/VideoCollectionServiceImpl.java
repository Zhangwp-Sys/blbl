package com.yilena.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.dao.VideoCollectionMapper;
import com.yilena.service.dao.VideoMapper;
import com.yilena.service.entity.dto.VideoCollectionDoOrUndoDTO;
import com.yilena.service.entity.po.VideoCollection;
import com.yilena.service.entity.vo.VideoVO;
import com.yilena.service.service.VideoCollectionService;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.SensitiveWordFilter;
import com.yilena.service.utils.SnowFlake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 视频合集服务实现类
 * 
 * 功能说明：
 * 1. 实现视频合集相关的业务逻辑
 * 2. 处理合集的创建、更新、删除
 * 3. 管理合集中的视频列表
 * 4. 支持合集可见性控制
 * 
 * 核心功能：
 * - 合集管理：创建、更新、删除视频合集
 * - 视频管理：添加视频到合集、从合集中移除视频
 * - 可见性控制：公开/私有合集设置
 * - 敏感词过滤：合集标题敏感词过滤
 * 
 */
@Service
@RequiredArgsConstructor
public class VideoCollectionServiceImpl implements VideoCollectionService {

    /**
     * 视频合集数据访问对象
     */
    private final VideoCollectionMapper videoCollectionMapper;

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 添加视频合集
     * 
     * 功能：创建新的视频合集
     * 业务逻辑：
     * 1. 设置合集默认值
     * 2. 过滤标题中的敏感词
     * 3. 保存到数据库
     * 
     * @param videoCollection 视频合集信息
     */
    @Override
    public void addVideoCollection(VideoCollection videoCollection) {
        videoCollection.setId(snowFlake.getID());
        videoCollection.setUserId(CurrentHolder.getCurrent());
        videoCollection.setIsDefault(StatusConstant.STATUS_NO);
        videoCollection.setVideoIdsJson("[]");
        videoCollection.setVisibility(videoCollection.getVisibility());
        videoCollection.setCreatedTime(LocalDateTime.now());
        videoCollection.setUpdatedTime(LocalDateTime.now());

        videoCollection.setTitle(SensitiveWordFilter.filterSensitiveWords(videoCollection.getTitle()));

        videoCollectionMapper.addVideoCollection(videoCollection);
    }

    /**
     * 获取视频合集列表
     * 
     * 功能：获取用户的视频合集列表
     * 业务逻辑：
     * 1. 查询用户的合集列表
     * 2. 如果不是本人，移除私有合集
     * 3. 解析视频ID JSON数据
     * 
     * @param userId 用户ID
     * @param currentUserId 当前用户ID
     * @return 视频合集列表
     */
    @Override
    public List<VideoCollection> getVideoCollectionByList(Long userId,Long currentUserId) {
        List<VideoCollection> videoCollections = videoCollectionMapper.getVideoCollectionByList(userId);
        if(!userId.equals(currentUserId)){
            videoCollections.removeIf(videoCollection -> Objects.equals(videoCollection.getVisibility(), StatusConstant.STATUS_NO));
        }
        // 便于前端计算视频数量
        videoCollections.forEach(videoCollection -> {
            videoCollection.setVideoIds(JSON.parseArray(videoCollection.getVideoIdsJson(), Long.class));
        });
        return videoCollections;
    }

    /**
     * 更新视频合集
     * 
     * 功能：更新视频合集信息
     * 业务逻辑：
     * 1. 验证合集是否存在
     * 2. 更新标题和可见性
     * 3. 设置更新时间
     * 
     * @param videoCollection 视频合集更新信息
     */
    @Override
    public void updateVideoCollection(VideoCollection videoCollection) {
        VideoCollection videoCollection1 = videoCollectionMapper.getVideoCollectionById(videoCollection.getId());
        if (videoCollection1 == null){
            throw new RuntimeException(StatusConstant.VIDEO_COLLECTION_NOT_EXIST);
        }
        videoCollection1.setTitle(videoCollection.getTitle());
        videoCollection1.setVisibility(videoCollection.getVisibility());
        videoCollection1.setUpdatedTime(LocalDateTime.now());
        videoCollectionMapper.updateVideoCollection(videoCollection1);
    }

    /**
     * 删除视频合集
     * 
     * @param id 视频合集ID
     */
    @Override
    public void deleteVideoCollection(Long id) {
        videoCollectionMapper.deleteVideoCollection(id);
    }

    /**
     * 添加视频到合集
     * 
     * 功能：将视频添加到指定合集
     * 业务逻辑：
     * 1. 验证合集是否存在
     * 2. 检查视频是否已在合集中
     * 3. 添加视频ID到合集
     * 
     * @param videoCollectionDoOrUndoDTO 操作信息
     */
    @Override
    public void doVideoCollection(VideoCollectionDoOrUndoDTO videoCollectionDoOrUndoDTO) {
        VideoCollection videoCollection = videoCollectionMapper.getVideoCollectionById(videoCollectionDoOrUndoDTO.getCollectionId());
        if(videoCollection == null){
            throw new RuntimeException(StatusConstant.VIDEO_COLLECTION_NOT_EXIST);
        }
        String videoIdsJson = videoCollection.getVideoIdsJson();
        List<Long> videoIds = JSON.parseArray(videoIdsJson, Long.class);
        videoIds.forEach(videoId -> {
            if (Objects.equals(videoId, videoCollectionDoOrUndoDTO.getVideoId())) {
                throw new RuntimeException(StatusConstant.VIDEO_ALREADY_COLLECTION);
            }
        });
        videoIds.add(videoCollectionDoOrUndoDTO.getVideoId());
        videoCollection.setVideoIdsJson(JSON.toJSONString(videoIds));

        videoCollectionMapper.updateVideoCollection(videoCollection);
    }

    /**
     * 从合集中移除视频
     * 
     * 功能：从合集中移除视频
     * 业务逻辑：
     * 1. 验证合集是否存在
     * 2. 查找并移除视频
     * 
     * @param videoCollectionDoOrUndoDTO 操作信息
     */
    @Override
    public void undoVideoCollection(VideoCollectionDoOrUndoDTO videoCollectionDoOrUndoDTO) {
        VideoCollection videoCollection = videoCollectionMapper.getVideoCollectionById(videoCollectionDoOrUndoDTO.getCollectionId());
        if(videoCollection == null){
            throw new RuntimeException(StatusConstant.VIDEO_COLLECTION_NOT_EXIST);
        }
        String videoIdsJson = videoCollection.getVideoIdsJson();
        List<Long> videoIds = JSON.parseArray(videoIdsJson, Long.class);

        boolean isFavorites = false;
        Iterator<Long> iterator = videoIds.iterator(); // 使用 Iterator 遍历
        while (iterator.hasNext()) {
            Long id = iterator.next();
            if (Objects.equals(id, videoCollectionDoOrUndoDTO.getVideoId())) {
                iterator.remove(); // 通过 Iterator 的 remove 方法安全移除
                isFavorites = true;
            }
        }
        if(!isFavorites){
            throw new RuntimeException(StatusConstant.VIDEO_NOT_EXIST);
        }

        videoCollection.setVideoIdsJson(JSON.toJSONString(videoIds));
        videoCollectionMapper.updateVideoCollection(videoCollection);
    }

    /**
     * 获取合集中的视频列表
     * 
     * 功能：获取指定合集中的视频列表
     * 业务逻辑：
     * 1. 验证合集是否存在
     * 2. 解析视频ID列表
     * 3. 查询视频详情
     * 4. 如果不是本人，移除私有视频
     * 
     * @param userId 当前用户ID
     * @param collectionId 合集ID
     * @return 视频列表
     */
    @Transactional
    @Override
    public List<VideoVO> getVideoIdsByCollectionId(Long userId, Long collectionId) {
        VideoCollection videoCollection = videoCollectionMapper.getVideoCollectionById(collectionId);
        if(videoCollection == null){
            throw new RuntimeException(StatusConstant.VIDEO_COLLECTION_NOT_EXIST);
        }
        String videoIdsJson = videoCollection.getVideoIdsJson();
        List<Long> videoIds = JSON.parseArray(videoIdsJson, Long.class);
        if(videoIds.isEmpty()){
            return List.of();
        }
        List<VideoVO> videoVOS = videoMapper.getVideoByIds(videoIds);
        if(!userId.equals(videoCollection.getUserId())){
            videoVOS.removeIf(videoVO -> Objects.equals(videoVO.getVisibility(), StatusConstant.STATUS_NO));
        }
        return videoVOS;
    }
}

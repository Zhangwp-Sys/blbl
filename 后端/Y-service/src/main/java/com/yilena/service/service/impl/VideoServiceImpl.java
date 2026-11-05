package com.yilena.service.service.impl;

import com.alibaba.fastjson2.JSON;
import com.yilena.service.constant.EntityConstant;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.dao.UserMapper;
import com.yilena.service.dao.VideoCollectionMapper;
import com.yilena.service.dao.VideoMapper;
import com.yilena.service.dao.VideoPendingMapper;
import com.yilena.service.entity.PageResult;
import com.yilena.service.entity.dto.VideoDTO;
import com.yilena.service.entity.po.History;
import com.yilena.service.entity.po.Video;
import com.yilena.service.entity.po.VideoCollection;
import com.yilena.service.entity.po.VideoPending;
import com.yilena.service.entity.vo.VideoVO;
import com.yilena.service.es.VideoES;
import com.yilena.service.service.VideoService;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.SensitiveWordFilter;
import com.yilena.service.utils.SnowFlake;
import com.yilena.service.utils.VideoDurationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 视频服务实现类
 * 
 * 功能说明：
 * 1. 实现视频相关的业务逻辑
 * 2. 处理视频投稿、审核、查询、更新、删除
 * 3. 集成Elasticsearch进行视频搜索
 * 4. 实现视频统计功能
 * 
 * 核心功能：
 * - 视频投稿：提交视频到审核队列
 * - 视频审核：处理视频审核状态变更
 * - 视频查询：分页查询、推荐查询、用户视频查询
 * - 视频管理：更新、删除视频信息
 * - 数据统计：视频播放量、点赞、投币等统计
 * 
 * 技术架构：
 * - 使用RabbitMQ异步同步视频数据到Elasticsearch
 * - 使用敏感词过滤工具保障内容安全
 * - 使用视频时长工具获取视频元信息
 * - 使用事务管理确保数据一致性
 * 
 */
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * RabbitMQ消息模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 视频Elasticsearch操作类
     */
    private final VideoES VideoES;

    /**
     * 待审核视频数据访问对象
     */
    private final VideoPendingMapper videoPendingMapper;

    /**
     * 视频合集数据访问对象
     */
    private final VideoCollectionMapper videoCollectionMapper;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 添加视频到审核队列
     * 
     * 功能：提交视频到待审核队列
     * 业务逻辑：
     * 1. 设置视频默认值（时间、统计数据）
     * 2. 验证视频URL和封面URL
     * 3. 过滤标题和描述中的敏感词
     * 4. 获取视频时长
     * 5. 将标签转换为JSON格式
     * 6. 保存到待审核表
     * 
     * @param video 视频信息
     * @return void
     * @throws RuntimeException 当视频URL为空、封面URL为空或视频处理失败时抛出异常
     */
    @Transactional
    @Override
    public void addVideoToPending(Video video) {
        // 设置默认值
        video.setUpdatedTime(LocalDateTime.now());
        video.setCreatedTime(LocalDateTime.now());
        video.setViews(0);
        video.setLikes(0);
        video.setFavorites(0);
        video.setShares(0);
        video.setCoins(0);
        video.setComments(0);
        video.setBarrages(0);
        video.setUserId(CurrentHolder.getCurrent());
        video.setId(snowFlake.getID());

        if (video.getVideoUrl() == null || video.getVideoUrl().isEmpty()) {
            throw new RuntimeException("视频URL不能为空，请先上传视频");
        }
        if (video.getCoverUrl() == null || video.getCoverUrl().isEmpty()) {
            throw new RuntimeException("视频封面URL不能为空，请先上传封面");
        }

        video.setDescription(SensitiveWordFilter.filterSensitiveWords(video.getDescription()));
        video.setTitle(SensitiveWordFilter.filterSensitiveWords(video.getTitle()));

        // 获取视频时长
        long time = VideoDurationUtils.getVideoDuration(video.getVideoUrl());
        video.setTime(time);

        // 将tags转成json
        String tagsJson = JSON.toJSONString(video.getTags());
        video.setTagsJson(tagsJson);

        videoPendingMapper.addVideoPending(video);
    }

    /**
     * 分页查询视频
     * 
     * 功能：分页查询视频列表
     * 业务逻辑：
     * 1. 使用Elasticsearch进行搜索
     * 2. 支持分页功能
     * 3. 返回分页结果
     * 
     * @param videoDTO 视频查询条件
     * @return 分页查询结果
     */
    @Override
    public PageResult<VideoVO> getVideoByPage(VideoDTO videoDTO) {
        return VideoES.getVideoByPage(videoDTO);
    }

    /**
     * 根据ID查询视频
     * 
     * 功能：根据视频ID查询视频详细信息
     * 业务逻辑：
     * 1. 查询视频信息
     * 2. 解析标签JSON数据
     * 3. 更新浏览量（非本人访问时）
     * 4. 获取合集名称
     * 5. 返回视频实体对象
     * 
     * @param id 视频ID
     * @return 视频实体对象
     * @throws NullPointerException 当视频不存在或合集不存在时抛出异常
     */
    @Transactional
    @Override
    public Video getVideoById(Long id) {
        Video video = videoMapper.getVideoById(id);
        // 将tags转成list
        String tagsJson = video.getTagsJson();
        video.setTags(JSON.parseArray(tagsJson, String.class));

        if(Objects.equals(video.getVisibility(), StatusConstant.STATUS_YES)) {
            // 浏览量加1
            if (!Objects.equals(CurrentHolder.getCurrent(), video.getUserId())) {
                video.setViews(video.getViews() + 1);
                videoMapper.updateVideo(video);
            }
        }

        VideoCollection videoCollection = videoCollectionMapper.getVideoCollectionById(video.getCollectionId());
        video.setCollectionName(videoCollection.getTitle());

//        // 如果用户已登录，则添加到历史记录
//        if(CurrentHolder.getCurrent() != null) {
//            // 添加到历史记录
//            History history = new History();
//            history.setId(snowFlake.getID());
//            history.setEntityType(EntityConstant.VIDEO_TYPE);
//            history.setEntityId(video.getId());
//            history.setCreatedTime(LocalDateTime.now());
//            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.HISTORY_BINDING_KEY, history);
//        }
        return video;
    }

    /**
     * 更新视频
     * 
     * 功能：更新视频信息
     * 业务逻辑：
     * 1. 设置更新时间
     * 2. 将标签转换为JSON格式
     * 3. 根据视频状态处理更新逻辑：
     *    - 待审核视频：更新审核表
     *    - 已审核视频：检查核心内容是否变更
     * 4. 处理合集变更逻辑
     * 5. 需要二次审核的视频移到审核表
     * 6. 同步更新到Elasticsearch
     * 
     * @param video 视频更新信息
     * @return void
     * @throws RuntimeException 当待审核视频尝试修改可见性、视频已在合集中、合集不存在或未做任何修改时抛出异常
     */
    @Transactional
    @Override
    public void updateVideo(Video video) {
        video.setUpdatedTime(LocalDateTime.now());
        String tagsJson = JSON.toJSONString(video.getTags());
        video.setTagsJson(tagsJson);
        Video oldVideo = videoMapper.getVideoById(video.getId());

        // 如果视频未通过审核，则不能修改未可见，而且所有变动均在审核表中进行
        if(oldVideo == null){
            if (Objects.equals(video.getVisibility(), StatusConstant.STATUS_YES)){
                throw new RuntimeException("待审核的视频无法修改可见权限！");
            }
            VideoPending videoPending = new VideoPending();
            BeanUtils.copyProperties(video, videoPending);
            videoPendingMapper.updateVideoPending(videoPending);
        }else {

            // 使用安全的空值比较判断核心字段是否变更（标题、描述、封面、视频、标签）
            boolean isCoreContentUnchanged = Objects.equals(video.getTitle(), oldVideo.getTitle())
                    && Objects.equals(video.getDescription(), oldVideo.getDescription())
                    && Objects.equals(video.getCoverUrl(), oldVideo.getCoverUrl())
                    && Objects.equals(video.getVideoUrl(), oldVideo.getVideoUrl());

            // 如果视频通过了审核，如果只有可见性和投稿合集变动则在全局视频中修改，反之会返回审核表进行二次审核
            if(isCoreContentUnchanged){

                // 核心内容未变更时，检查可见性和合集ID是否修改
                boolean isVisibilityUnchanged = Objects.equals(video.getVisibility(), oldVideo.getVisibility());
                boolean isCollectionUnchanged = Objects.equals(video.getCollectionId(), oldVideo.getCollectionId());

                if(isVisibilityUnchanged && isCollectionUnchanged){
                    throw new RuntimeException("您没有做出修改！");
                }
                // 无需二次审核
                else {
                    videoMapper.updateVideo(video);
                    VideoVO videoVO = new VideoVO();
                    BeanUtils.copyProperties(video, videoVO);
                    videoVO.setType(MqConstant.SEND_PUT);
                    rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.VIDEO_BINDING_KEY, videoVO);
                }
            }else{
                // 判断是否更换了收藏夹
                if(!oldVideo.getCollectionId().equals(video.getCollectionId())){
                    // 更新新的收藏夹ids
                    VideoCollection videoCollection = videoCollectionMapper.getVideoCollectionById(video.getCollectionId());
                    List<Long> videoIds = JSON.parseArray(videoCollection.getVideoIdsJson(), Long.class);
                    videoIds.forEach(videoId -> {
                        if (Objects.equals(videoId, video.getId())) {
                            throw new RuntimeException(StatusConstant.VIDEO_ALREADY_COLLECTION);
                        }
                    });
                    videoIds.add(video.getId());
                    videoCollection.setVideoIdsJson(JSON.toJSONString(videoIds));
                    videoCollectionMapper.updateVideoCollection(videoCollection);

                    // 更新旧的收藏夹ids
                    VideoCollection oldVideoCollection = videoCollectionMapper.getVideoCollectionById(oldVideo.getCollectionId());
                    List<Long> oldVideoIds = JSON.parseArray(oldVideoCollection.getVideoIdsJson(), Long.class);
                    boolean isExist = false;
                    for (int i = 0; i < oldVideoIds.size(); i++) {
                        if(Objects.equals(oldVideoIds.get(i), video.getId())){
                            oldVideoIds.remove(i);
                            isExist = true;
                            break;
                        }
                    }
                    if(isExist){
                        oldVideoCollection.setVideoIdsJson(JSON.toJSONString(oldVideoIds));
                        videoCollectionMapper.updateVideoCollection(oldVideoCollection);
                    }else{
                        throw new RuntimeException(StatusConstant.VIDEO_NOT_EXIST);
                    }
                }

                // 将视频移到待审核表
                videoMapper.updateVideo(video);
                Video oldVideo1 = videoMapper.getVideoById(video.getId());
                oldVideo1.setVisibility(StatusConstant.STATUS_NO);
                oldVideo1.setId(snowFlake.getID());

                //记录本次视频id
                oldVideo1.setLastId(video.getId());

                videoPendingMapper.addVideoPending(oldVideo1);
                videoMapper.deleteVideo(video.getId());

                // 发送消息删除es索引
                VideoVO videoVO = new VideoVO();
                BeanUtils.copyProperties(video, videoVO);
                videoVO.setType(MqConstant.SEND_DELETE);
                rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.VIDEO_BINDING_KEY, videoVO);
            }
        }
    }

    /**
     * 删除视频
     * 
     * 功能：删除指定视频
     * 业务逻辑：
     * 1. 查询视频是否存在
     * 2. 如果视频已审核通过，删除全局视频
     * 3. 如果视频未审核，删除待审核视频
     * 4. 同步删除操作到Elasticsearch
     * 
     * @param id 视频ID
     * @return void
     */
    @Override
    public void deleteVideo(Long id) {
        Video video = videoMapper.getVideoById(id);

        // 如果视频通过审核，则删除全局视频，反之删除待审核视频
        if(video != null) {
            videoMapper.deleteVideo(id);

            VideoVO videoVO = new VideoVO();
            videoVO.setId(id);
            videoVO.setType(MqConstant.SEND_DELETE);
            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.VIDEO_BINDING_KEY, videoVO);
        }else{
            videoPendingMapper.deleteVideoPending(id);
        }
    }

    /**
     * 获取所有视频
     * 
     * 功能：获取所有视频列表并随机排序
     * 业务逻辑：
     * 1. 从Elasticsearch获取所有视频
     * 2. 随机打乱顺序
     * 3. 返回视频列表
     * 
     * @return 视频列表
     */
    @Override
    public List<VideoVO> getAllVideos() {
        List<VideoVO> videoVOs =  VideoES.getAllVideos();
        Collections.shuffle(videoVOs);
        return videoVOs;
    }

    /**
     * 更新视频弹幕数
     * 
     * 功能：更新视频的弹幕数量
     * 业务逻辑：
     * 1. 根据视频ID查询视频
     * 2. 更新弹幕数量
     * 3. 保存到数据库
     * 4. 同步更新到Elasticsearch
     * 
     * @param videoId 视频ID
     * @param danmuCount 弹幕数量
     * @return void
     * @throws NumberFormatException 当videoId格式不正确时抛出异常
     * @throws NullPointerException 当视频不存在时抛出异常
     */
    @Transactional
    @Override
    public void updateVideoDanmuCount(String videoId, Long danmuCount) {
        Video video = videoMapper.getVideoById(Long.parseLong(videoId));
        video.setBarrages(Math.toIntExact(danmuCount));
        videoMapper.updateVideo(video);

        VideoVO videoVO = new VideoVO();
        videoVO.setId(Long.parseLong(videoId));
        videoVO.setBarrages(Math.toIntExact(danmuCount));
        videoVO.setType(MqConstant.SEND_PUT);
        rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.VIDEO_BINDING_KEY, videoVO);
    }

    /**
     * 获取左侧推荐视频
     * 
     * 功能：获取左侧推荐栏的视频列表
     * 业务逻辑：
     * 1. 查询推荐视频列表
     * 2. 填充用户名信息
     * 3. 返回视频列表
     * 
     * @return 视频列表
     * @throws NullPointerException 当视频对应的用户不存在时抛出异常
     */
    @Override
    public List<VideoVO> getLeftRecommendVideos() {
        List<VideoVO> videoVOS =  videoMapper.getLeftRecommendVideos();
        for(VideoVO videoVO : videoVOS){
            videoVO.setUsername(userMapper.getUserById(videoVO.getUserId()).getUsername());
        }
        return videoVOS;
    }

    /**
     * 获取用户视频总播放量
     * 
     * @param userId 用户ID
     * @return 总播放量
     */
    @Override
    public Long getUserVideoAllViews(Long userId) {
        Long views = videoMapper.getUserVideoAllViews(userId);
        return views == null ? 0 : views;
    }

    /**
     * 获取用户视频总投币数
     * 
     * @param userId 用户ID
     * @return 总投币数
     */
    @Override
    public Long getUserVideoAllCoins(Long userId) {
        Long coins = videoMapper.getUserVideoAllCoins(userId);
        return coins == null ? 0 : coins;
    }

    /**
     * 获取用户视频总评论数
     * 
     * @param userId 用户ID
     * @return 总评论数
     */
    @Override
    public Long getUserVideoAllComments(Long userId) {
        Long comments = videoMapper.getUserVideoAllComments(userId);
        return comments == null ? 0 : comments;
    }

    /**
     * 获取用户视频总弹幕数
     * 
     * @param userId 用户ID
     * @return 总弹幕数
     */
    @Override
    public Long getUserVideoAllBarrages(Long userId) {
        Long barrages = videoMapper.getUserVideoAllBarrages(userId);
        return barrages == null ? 0 : barrages;
    }

    /**
     * 获取用户视频总点赞数
     * 
     * @param userId 用户ID
     * @return 总点赞数
     */
    @Override
    public Long getUserVideoAllLikes(Long userId) {
        Long likes = videoMapper.getUserVideoAllLikes(userId);
        return likes == null ? 0 : likes;
    }

    /**
     * 获取用户视频总分享数
     * 
     * @param userId 用户ID
     * @return 总分享数
     */
    @Override
    public Long getUserVideoAllShares(Long userId) {
        Long shares = videoMapper.getUserVideoAllShares(userId);
        return shares == null ? 0 : shares;
    }

    /**
     * 获取用户视频总收藏数
     * 
     * @param userId 用户ID
     * @return 总收藏数
     */
    @Override
    public Long getUserVideoAllFavorites(Long userId) {
        Long favorites = videoMapper.getUserVideoAllFavorites(userId);
        return favorites == null ? 0 : favorites;
    }

    /**
     * 根据用户ID获取视频列表
     * 
     * @param userId 用户ID
     * @return 视频列表
     */
    @Override
    public List<Video> getVideoByUserId(Long userId) {
        return videoMapper.getVideoByUserId(userId);
    }

    /**
     * 获取用户信息页视频列表
     * 
     * 功能：根据排序类型获取用户视频列表
     * 业务逻辑：
     * 1. 根据排序类型查询视频列表
     * 2. 返回视频列表
     * 
     * @param userId 用户ID
     * @param sortType 排序类型（最新、播放量、收藏数）
     * @return 视频列表
     */
    @Override
    public List<VideoVO> getUserInfoVideos(Long userId, Integer sortType) {
        if(sortType.equals(EntityConstant.VIDEO_NEW_TYPE)) {
            return videoMapper.getUserInfoVideosByNew(userId);
        }else if(sortType.equals(EntityConstant.VIDEO_VIEWS_TYPE)){
            return videoMapper.getUserInfoVideosByViews(userId);
        }else if(sortType.equals(EntityConstant.VIDEO_FAVORITES_TYPE)){
            return videoMapper.getUserInfoVideosByFavorites(userId);
        }
        return List.of();
    }
}

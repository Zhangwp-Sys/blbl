package com.yilena.service.service.impl;

import com.alibaba.fastjson2.JSON;
import com.yilena.service.constant.*;
import com.yilena.service.dao.FavoritesFolderMapper;
import com.yilena.service.dao.VideoFavoritesMapper;
import com.yilena.service.dao.VideoMapper;
import com.yilena.service.entity.PageResult;
import com.yilena.service.entity.dto.FavoritesFolderDoOrUndoDTO;
import com.yilena.service.entity.dto.FavoritesFolderVideosDTO;
import com.yilena.service.entity.po.ChatMessage;
import com.yilena.service.entity.po.FavoritesFolder;
import com.yilena.service.entity.po.Video;
import com.yilena.service.entity.po.VideoFavorites;
import com.yilena.service.entity.vo.FavoritesFolderVO;
import com.yilena.service.entity.vo.VideoVO;
import com.yilena.service.es.VideoES;
import com.yilena.service.service.FavoritesFolderService;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.SensitiveWordFilter;
import com.yilena.service.utils.SnowFlake;
import com.yilena.service.webSocket.ChatEndpoint;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * 收藏夹服务实现类
 * 
 * 功能说明：
 * 1. 实现收藏夹相关的业务逻辑
 * 2. 处理收藏夹的创建、更新、删除
 * 3. 实现视频收藏和取消收藏
 * 4. 实现收藏夹订阅功能
 * 
 * 核心功能：
 * - 收藏夹管理：创建、更新、删除收藏夹
 * - 视频收藏：将视频添加到收藏夹或从收藏夹移除
 * - 订阅功能：订阅他人的公开收藏夹
 * - 通知功能：向订阅者发送新视频通知
 * 
 * 技术架构：
 * - 使用RabbitMQ异步同步收藏数据到Elasticsearch
 * - 使用WebSocket发送订阅通知
 * - 使用敏感词过滤工具保障内容安全
 * - 使用事务管理确保数据一致性
 * 
 */
@Service
@RequiredArgsConstructor
public class FavoritesFolderServiceImpl implements FavoritesFolderService {

    /**
     * 收藏夹数据访问对象
     */
    private final FavoritesFolderMapper favoritesFolderMapper;

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * 视频Elasticsearch操作类
     */
    private final VideoES videoES;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * RabbitMQ消息模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 视频收藏数据访问对象
     */
    private final VideoFavoritesMapper videoFavoritesMapper;

    /**
     * 聊天WebSocket端点
     */
    private final ChatEndpoint chatEndpoint;

    /**
     * Redis模板对象
     */
    private final RedisTemplate redisTemplate;

    /**
     * 添加收藏夹
     * 
     * 功能：创建新的收藏夹
     * 业务逻辑：
     * 1. 设置收藏夹默认值
     * 2. 过滤标题中的敏感词
     * 3. 处理封面设置（自定义或自动）
     * 4. 保存到数据库
     * 
     * @param favoritesFolder 收藏夹信息
     */
    @Override
    public void addFavoritesFolder(FavoritesFolder favoritesFolder) {
        favoritesFolder.setId(snowFlake.getID());
        favoritesFolder.setUpdatedTime(LocalDateTime.now());
        favoritesFolder.setCreatedTime(LocalDateTime.now());
        favoritesFolder.setUserId(CurrentHolder.getCurrent());
        favoritesFolder.setIsDefault(StatusConstant.STATUS_NO);
        favoritesFolder.setVideoIdsJson("[]");
        favoritesFolder.setVideoCount(0);
        favoritesFolder.setSubscribersJson("[]");
        favoritesFolder.setClicks(0);

        favoritesFolder.setTitle(SensitiveWordFilter.filterSensitiveWords(favoritesFolder.getTitle()));

        // 判断是否自定义封面
        String coverUrl = favoritesFolder.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            favoritesFolder.setIsCoverAuto(StatusConstant.STATUS_NO);
            favoritesFolder.setCoverUrl(coverUrl);
        } else {
            favoritesFolder.setIsCoverAuto(StatusConstant.STATUS_YES);
            favoritesFolder.setCoverUrl(CoverUrlConstant.NULL);
        }

        favoritesFolderMapper.addFavoritesFolder(favoritesFolder);
    }

    /**
     * 获取当前用户的收藏夹列表
     * 
     * @return 收藏夹列表
     */
    @Override
    public List<FavoritesFolderVO> getFavoritesFolderByList() {
        FavoritesFolder favoritesFolder = new FavoritesFolder();
        favoritesFolder.setUserId(CurrentHolder.getCurrent());
        return favoritesFolderMapper.getFavoritesFolderByList(favoritesFolder);
    }

    /**
     * 更新收藏夹
     * 
     * 功能：更新收藏夹信息
     * 业务逻辑：
     * 1. 设置更新时间
     * 2. 处理封面设置（自定义或自动更新）
     * 3. 保存到数据库
     * 
     * @param favoritesFolder 收藏夹更新信息
     */
    @Override
    public void updateFavoritesFolder(FavoritesFolder favoritesFolder) {
        favoritesFolder.setUpdatedTime(LocalDateTime.now());
        // 判断是否自定义封面
        String coverUrl = favoritesFolder.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            favoritesFolder.setIsCoverAuto(StatusConstant.STATUS_NO);
        } else{
            favoritesFolder.setIsCoverAuto(StatusConstant.STATUS_YES);

            // 更换最新封面
            FavoritesFolder oldFavoritesFolder = favoritesFolderMapper.getFavoritesFolderById(favoritesFolder.getId());
            List<Long> videoIds = JSON.parseArray(oldFavoritesFolder.getVideoIdsJson(), Long.class);
            Video video = videoMapper.getVideoById(videoIds.get(videoIds.size() - 1));
            favoritesFolder.setCoverUrl(video.getCoverUrl());
        }
        favoritesFolderMapper.updateFavoritesFolder(favoritesFolder);
    }

    /**
     * 收藏视频
     * 
     * 功能：将视频添加到收藏夹
     * 业务逻辑：
     * 1. 验证视频是否存在
     * 2. 检查是否已收藏
     * 3. 更新收藏夹视频列表
     * 4. 更新视频收藏数
     * 5. 更新收藏夹封面（如自动更新）
     * 6. 同步收藏记录到Elasticsearch
     * 7. 向订阅者发送通知
     * 
     * @param favoritesFolderDoOrUndoDTO 收藏操作信息
     * @throws RuntimeException 视频不存在或已收藏时抛出异常
     */
    @Transactional
    @Override
    public void doFavoritesFolder(FavoritesFolderDoOrUndoDTO favoritesFolderDoOrUndoDTO) {
        FavoritesFolder favoritesFolder = favoritesFolderMapper.getFavoritesFolderById(favoritesFolderDoOrUndoDTO.getFavoritesId());
        // 判断视频是否存在
        Video video = videoMapper.getVideoById(favoritesFolderDoOrUndoDTO.getId());
        if(video == null){
            throw new RuntimeException(StatusConstant.VIDEO_NOT_EXIST);
        }
        // 将json转化为list
        String videoIdsJson = favoritesFolder.getVideoIdsJson();
        List<Long> videoIds = JSON.parseArray(videoIdsJson, Long.class);
        // 判断是否已经收藏过
        videoIds.forEach(videoId -> {
            if (Objects.equals(videoId, favoritesFolderDoOrUndoDTO.getId())) {
                throw new RuntimeException(FavoritesConstant.VIDEO_ALREADY_FAVORITES);
            }
        });
        videoIds.add(favoritesFolderDoOrUndoDTO.getId());

        // 更新视频收藏数
        video.setFavorites(video.getFavorites() + 1);
        videoMapper.updateVideo(video);

        favoritesFolder.setVideoIdsJson(JSON.toJSONString(videoIds));
        favoritesFolder.setVideoCount(videoIds.size());
        if(Objects.equals(favoritesFolder.getIsCoverAuto(), StatusConstant.STATUS_YES)){
            favoritesFolder.setCoverUrl(video.getCoverUrl());
        }
        favoritesFolderMapper.updateFavoritesFolder(favoritesFolder);

        // 添加视频收藏记录
        VideoFavorites videoFavorites = new VideoFavorites();
        videoFavorites.setId(snowFlake.getID());
        videoFavorites.setVideoId(favoritesFolderDoOrUndoDTO.getId());
        videoFavorites.setFolderId(favoritesFolder.getId());
        videoFavorites.setUserId(CurrentHolder.getCurrent());
        videoFavorites.setCreatedTime(LocalDateTime.now());
        videoFavorites.setUpdatedTime(LocalDateTime.now());
        videoFavorites.setType(MqConstant.SEND_ADD);
        rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.VIDEO_FAVORITES_BINDING_KEY, videoFavorites);

        //群发给订阅者
        if(favoritesFolder.getVisibility().equals(StatusConstant.STATUS_YES)){
            List<Long> subscribers = JSON.parseArray(favoritesFolder.getSubscribersJson(), Long.class);
            subscribers.forEach(subscriber -> {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setCreatedTime(LocalDateTime.now());
                chatMessage.setSendUserId(favoritesFolder.getUserId());
                chatMessage.setReceiveUserId(subscriber);
                chatMessage.setType(MessageTypeConstant.FAVORITES);
                chatMessage.setVisibilityBySender(StatusConstant.STATUS_NO);
                chatMessage.setVisibilityByReceiver(StatusConstant.STATUS_YES);
                chatMessage.setContent("你订阅的收藏夹" + favoritesFolder.getTitle() + "有新的视频，快去查看吧！");
                chatMessage.setId(snowFlake.getID());
                String json = com.alibaba.fastjson.JSON.toJSONString(chatMessage);
                chatEndpoint.sendMessage(json);
            });
        }
    }

    /**
     * 取消收藏视频
     * 
     * 功能：从收藏夹中移除视频
     * 业务逻辑：
     * 1. 验证视频是否存在
     * 2. 查找收藏记录
     * 3. 从收藏夹视频列表中移除
     * 4. 更新视频收藏数
     * 5. 更新收藏夹封面（如自动更新）
     * 6. 删除收藏记录
     * 
     * @param favoritesFolderDoOrUndoDTO 取消收藏操作信息
     * @throws RuntimeException 视频不存在或未收藏时抛出异常
     */
    @Transactional
    @Override
    public void unDoFavoritesFolder(FavoritesFolderDoOrUndoDTO favoritesFolderDoOrUndoDTO) {
        // 判断视频是否存在
        Video video = videoMapper.getVideoById(favoritesFolderDoOrUndoDTO.getId());
        if(video == null){
            throw new RuntimeException(StatusConstant.VIDEO_NOT_EXIST);
        }

        // 判断是否已经收藏过
        VideoFavorites videoFavorites = videoFavoritesMapper.isFavoritesFolder(favoritesFolderDoOrUndoDTO.getId(), CurrentHolder.getCurrent());
        FavoritesFolder favoritesFolder = favoritesFolderMapper.getFavoritesFolderById(videoFavorites.getFolderId());
        // 将json转化为list
        String videoIdsJson = favoritesFolder.getVideoIdsJson();
        List<Long> videoIds = JSON.parseArray(videoIdsJson, Long.class);
        // 判断是否已经收藏过，并安全移除元素
        boolean isFavorites = false;
        Iterator<Long> iterator = videoIds.iterator(); // 使用 Iterator 遍历
        while (iterator.hasNext()) {
            Long id = iterator.next();
            if (Objects.equals(id, favoritesFolderDoOrUndoDTO.getId())) {
                iterator.remove(); // 通过 Iterator 的 remove 方法安全移除
                isFavorites = true;
            }
        }
        if(!isFavorites){
            throw new RuntimeException(FavoritesConstant.VIDEO_NOT_ALREADY_FAVORITES);
        }

        // 更新视频收藏数
        video.setFavorites(video.getFavorites() - 1);
        videoMapper.updateVideo(video);

        // 设置封面
        if(!videoIds.isEmpty()) {
            Video lastVideo = videoMapper.getVideoById(videoIds.get(videoIds.size() - 1));
            if(Objects.equals(favoritesFolder.getIsCoverAuto(), StatusConstant.STATUS_NO)){
                favoritesFolder.setCoverUrl(lastVideo.getCoverUrl());
            }
            favoritesFolder.setVideoIdsJson(JSON.toJSONString(videoIds));
            favoritesFolder.setVideoCount(videoIds.size());
        }else{
            favoritesFolder.setCoverUrl(CoverUrlConstant.NULL);
            favoritesFolder.setVideoIdsJson("[]");
            favoritesFolder.setVideoCount(videoIds.size());
        }
        // 更新收藏夹视频数
        favoritesFolder.setVideoCount(videoIds.size());

        favoritesFolderMapper.updateFavoritesFolder(favoritesFolder);

        videoFavoritesMapper.deleteVideoFavorites(videoFavorites);
    }

    /**
     * 根据用户ID获取收藏夹列表
     * 
     * 功能：获取指定用户的公开收藏夹列表
     * 业务逻辑：
     * 1. 查询用户的收藏夹列表
     * 2. 移除不可见的收藏夹
     * 3. 返回可见收藏夹列表
     * 
     * @param userId 用户ID
     * @return 收藏夹列表
     */
    @Override
    public List<FavoritesFolderVO> getFavoritesFolderById(Long userId) {
        FavoritesFolder favoritesFolder = new FavoritesFolder();
        favoritesFolder.setUserId(userId);
        List<FavoritesFolderVO> favoritesFolderVOS = favoritesFolderMapper.getOthersFavoritesFolderByList(favoritesFolder);
        if(favoritesFolderVOS.isEmpty()){
            return List.of();
        }

        // 移除不可见收藏夹
            for (int i = 0; i < favoritesFolderVOS.size(); i++) {
                if (Objects.equals(favoritesFolderVOS.get(i).getVisibility(), StatusConstant.STATUS_NO)) {
                    favoritesFolderVOS.remove(i);
                    i--;
                }
            }
        return favoritesFolderVOS;
    }

    /**
     * 分页获取收藏夹中的视频
     * 
     * 功能：分页获取收藏夹中的视频列表
     * 业务逻辑：
     * 1. 获取收藏夹的视频ID列表
     * 2. 使用Elasticsearch查询视频详情
     * 3. 更新收藏夹点击量（非本人访问时）
     * 4. 返回分页结果
     * 
     * @param favoritesFolderVideosDTO 查询条件
     * @param userId 当前用户ID
     * @return 视频分页结果
     */
    @Override
    public PageResult<VideoVO> getVideosFromFavoritesFolderByPage(FavoritesFolderVideosDTO favoritesFolderVideosDTO, Long userId) {
        String videoIdsJson = favoritesFolderMapper.getFavoritesFolderById(favoritesFolderVideosDTO.getId()).getVideoIdsJson();
        List<Long> videoIds = JSON.parseArray(videoIdsJson, Long.class);
        if(videoIds.isEmpty()){
            return new PageResult<>();
        }
        favoritesFolderVideosDTO.setVideoIds(videoIds);
        PageResult<VideoVO> videos = videoES.getVideosByIds(favoritesFolderVideosDTO);

        // 更新收藏夹点击量
        FavoritesFolder favoritesFolder = favoritesFolderMapper.getFavoritesFolderById(favoritesFolderVideosDTO.getId());
        if(!Objects.equals(userId, favoritesFolder.getUserId())) {
            favoritesFolder.setClicks(favoritesFolder.getClicks() + 1);
            favoritesFolderMapper.updateFavoritesFolder(favoritesFolder);
        }
        return videos;
    }

    /**
     * 删除收藏夹
     * 
     * @param id 收藏夹ID
     */
    @Override
    public void deleteFavoritesFolder(Long id) {
        favoritesFolderMapper.deleteFavoritesFolder(id);
    }

    /**
     * 订阅他人收藏夹
     * 
     * 功能：订阅他人的公开收藏夹
     * 业务逻辑：
     * 1. 验证收藏夹是否存在
     * 2. 验证是否为默认收藏夹（默认收藏夹不可订阅）
     * 3. 验证是否订阅自己
     * 4. 检查是否已订阅
     * 5. 添加订阅者到列表
     * 
     * @param id 收藏夹ID
     * @throws RuntimeException 收藏夹不存在、不可订阅或已订阅时抛出异常
     */
    @Override
    public void subscribeOthersFavoritesFolder(Long id) {
        FavoritesFolder favoritesFolder = favoritesFolderMapper.getFavoritesFolderById(id);
        // 判断是否存在
        if(favoritesFolder == null){
            throw new RuntimeException(FavoritesConstant.FAVORITES_NOT_EXIST);
        }
        // 判断是否是默认收藏夹
        if(Objects.equals(favoritesFolder.getIsDefault(), StatusConstant.STATUS_YES)){
            throw new RuntimeException(FavoritesConstant.FAVORITES_NOT_DEFAULT);
        }
        // 判断是否是自己
        if(Objects.equals(favoritesFolder.getUserId(), CurrentHolder.getCurrent())){
            throw new RuntimeException(FavoritesConstant.FAVORITES_CANNOT_SUBSCRIBE_SELF);
        }
        // 判断是否已经订阅过
        String subscribersJson = favoritesFolder.getSubscribersJson();
        List<Long> subscribers = JSON.parseArray(subscribersJson, Long.class);
        if(subscribers.contains(CurrentHolder.getCurrent())){
            throw new RuntimeException(FavoritesConstant.FAVORITES_ALREADY_SUBSCRIBE);
        }
        subscribers.add(CurrentHolder.getCurrent());
        favoritesFolder.setSubscribersJson(JSON.toJSONString(subscribers));
        favoritesFolderMapper.updateFavoritesFolder(favoritesFolder);
    }

    /**
     * 检查视频是否已收藏
     * 
     * @param videoId 视频ID
     * @param userId 用户ID
     * @return 收藏记录（null表示未收藏）
     */
    @Override
    public VideoFavorites isFavoritesFolder(Long videoId, Long userId) {
        VideoFavorites videoFavorites =  videoFavoritesMapper.isFavoritesFolder(videoId, userId);
//        if(videoFavorites == null){
//            Video video = videoMapper.getVideoById(videoId);
//            if(video != null){
//                Long oldId = video.getLastId();
//                videoFavorites = videoFavoritesMapper.isFavoritesFolder(oldId, userId);
//                if(videoFavorites != null){
//                    // 删除旧记录
//                    VideoFavorites videoFavorites1 = new VideoFavorites();
//                    videoFavorites1.setVideoId(oldId);
//                    videoFavorites1.setUserId(userId);
//                    videoFavoritesMapper.deleteVideoFavorites(videoFavorites1);
//
//                    videoFavorites1.setId(snowFlake.getID());
//                    videoFavorites1.setFolderId(videoFavorites.getFolderId());
//                    videoFavorites1.setCreatedTime(videoFavorites.getCreatedTime());
//                    videoFavorites1.setUpdatedTime(videoFavorites.getUpdatedTime());
//                    videoFavoritesMapper.addVideoFavorites(videoFavorites1);
//                }
//            }
//        }
        return videoFavorites;
    }

    /**
     * 获取用户订阅的收藏夹列表
     * 
     * @param userId 用户ID
     * @return 订阅的收藏夹列表
     */
    @Override
    public List<FavoritesFolderVO> getSubscribeFavoritesFolder(Long userId) {
        List<FavoritesFolderVO> favoritesFolderVOS = favoritesFolderMapper.getSubscribeFavoritesFolder(userId);
        if(favoritesFolderVOS.isEmpty()){
            return List.of();
        }
        return favoritesFolderVOS;
    }

    /**
     * 检查是否已订阅收藏夹
     * 
     * @param userId 用户ID
     * @param favoritesFolderId 收藏夹ID
     * @return 收藏夹信息（null表示未订阅）
     */
    @Override
    public FavoritesFolder isSubscribe(Long userId, Long favoritesFolderId) {
        FavoritesFolder favoritesFolder = favoritesFolderMapper.getFavoritesFolderById(favoritesFolderId);
        List<Long> subscribers = JSON.parseArray(favoritesFolder.getSubscribersJson(), Long.class);
        if(subscribers.contains(userId)){
            return favoritesFolder;
        }else{
            return null;
        }
    }

    /**
     * 取消订阅收藏夹
     * 
     * 功能：取消订阅他人的收藏夹
     * 业务逻辑：
     * 1. 验证收藏夹是否存在
     * 2. 验证是否已订阅
     * 3. 从订阅者列表中移除
     * 
     * @param id 收藏夹ID
     * @throws RuntimeException 收藏夹不存在或未订阅时抛出异常
     */
    @Override
    public void unsubscribeOthersFavoritesFolder(Long id) {
        FavoritesFolder favoritesFolder = favoritesFolderMapper.getFavoritesFolderById(id);
        if(favoritesFolder == null){
            throw new RuntimeException(FavoritesConstant.FAVORITES_NOT_EXIST);
        }
        List<Long> subscribers = JSON.parseArray(favoritesFolder.getSubscribersJson(), Long.class);
        if(!subscribers.contains(CurrentHolder.getCurrent())){
            throw new RuntimeException(FavoritesConstant.FAVORITES_NOT_SUBSCRIBE);
        }
        subscribers.remove(CurrentHolder.getCurrent());
        favoritesFolder.setSubscribersJson(JSON.toJSONString(subscribers));
        favoritesFolderMapper.updateFavoritesFolder(favoritesFolder);
    }
}

package com.yilena.service.dao;

import com.yilena.service.entity.po.VideoFavorites;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

/**
 * 视频收藏数据访问接口
 * 
 * 功能说明：
 * 1. 提供视频收藏功能的数据访问
 * 2. 支持添加和取消收藏
 * 3. 支持查询用户是否已收藏
 * 4. 支持统计总收藏数
 * 
 */
@Mapper
public interface VideoFavoritesMapper {
    void addVideoFavorites(VideoFavorites videoFavorites);

    void deleteVideoFavorites(VideoFavorites videoFavorites);

    VideoFavorites isFavoritesFolder(Long videoId, Long userId);

    Long getTotalFavorite();
}

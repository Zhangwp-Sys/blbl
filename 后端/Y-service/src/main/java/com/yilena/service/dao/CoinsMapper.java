package com.yilena.service.dao;

import com.yilena.service.entity.po.VideoCoins;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 投币数据访问接口
 * 
 * 功能说明：
 * 1. 提供投币和取消投币功能
 * 2. 支持获取用户投过的视频列表
 * 3. 支持查询用户是否已投币
 * 
 */
@Mapper
public interface CoinsMapper {
    void addCoins(VideoCoins videoCoins);

    List<Long> getVideoCoins(Long userId);

    VideoCoins isCoins(Long videoId, Long userId);

    void deleteCoins(VideoCoins videoCoins1);
}

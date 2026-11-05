package com.yilena.service.dao;

import com.yilena.service.entity.po.VideoCollection;
import com.yilena.service.entity.vo.VideoVO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 视频合集数据访问接口
 * 
 * 功能说明：
 * 1. 提供视频合集的增删改查
 * 2. 支持查询用户的合集列表
 * 3. 支持根据ID查询合集
 * 
 */
@Mapper
public interface VideoCollectionMapper {
    void addVideoCollection(VideoCollection videoCollection);

    List<VideoCollection> getVideoCollectionByList(Long userId);

    VideoCollection getVideoCollectionById(Long id);

    void updateVideoCollection(VideoCollection videoCollection1);

    void deleteVideoCollection(Long id);
}

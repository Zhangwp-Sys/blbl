package com.yilena.service.dao;

import com.yilena.service.entity.po.Like;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;

/**
 * 点赞数据访问接口
 * 
 * 功能说明：
 * 1. 提供点赞和取消点赞功能
 * 2. 支持查询是否已点赞
 * 3. 支持统计总点赞数
 * 
 */
@Mapper
public interface LikeMapper {
    void doLike(Like like);

    void undoLike(Like like);

    Like isLike(Like like);

    Long getTotalLike();
}

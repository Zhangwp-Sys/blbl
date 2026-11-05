package com.yilena.service.dao;

import com.yilena.service.entity.po.Share;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分享记录数据访问接口
 * 
 * 功能说明：
 * 1. 提供分享记录的添加和查询
 * 2. 支持查询是否已分享
 * 
 */
@Mapper
public interface ShareMapper {
    void addShare(Share share);

    Share getIsShare(Share share);
}

package com.yilena.service.dao;

import com.yilena.service.entity.po.HotSearch;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 热搜数据访问接口
 * 
 * 功能说明：
 * 1. 提供热搜列表查询
 * 2. 支持更新搜索次数
 * 3. 支持新增热搜记录
 * 4. 支持删除搜索次数低于100的冷门搜索词
 * 
 */
@Mapper
public interface HotSearchMapper {
    List<HotSearch> getHotSearchVideo();

    void updateSearchHistory(HotSearch hsv);

    void addSearchHistory(HotSearch hsv);

    HotSearch getHotSearchVideoByContent(String content);

    void deleteHotSearchLtHundredCount();
}

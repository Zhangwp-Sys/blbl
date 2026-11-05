package com.yilena.service.dao;

import com.yilena.service.entity.dto.HistoryDTO;
import com.yilena.service.entity.po.History;
import com.yilena.service.entity.vo.VideoVO;
import org.apache.ibatis.annotations.MapKey;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 浏览历史数据访问接口
 * 
 * 功能说明：
 * 1. 提供浏览历史的增删查
 * 2. 支持按日期分组查询历史
 * 3. 支持删除单条和全部历史
 * 4. 支持删除15天前的过期记录
 * 
 */
@Mapper
public interface HistoryMapper {
    void addHistory(History history);

    History getHistoryByUserIdAndEntityId(Integer entityType, Long entityId, Long userId);

    void deleteHistory(Long id);

    @MapKey("dateStr")
    Map<String, HistoryDTO> getHistory(Long userId);

    void deleteAllHistory(Long userId);

    void deleteHistoryFifteenDaysAgo();
}

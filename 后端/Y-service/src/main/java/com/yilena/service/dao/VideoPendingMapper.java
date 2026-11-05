package com.yilena.service.dao;

import com.yilena.service.entity.dto.VideoPendingDTO;
import com.yilena.service.entity.po.Video;
import com.yilena.service.entity.po.VideoPending;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频待审核数据访问接口
 * 
 * 功能说明：
 * 1. 提供视频待审核记录的增删改查
 * 2. 支持按状态查询视频列表
 * 3. 支持统计各状态下的视频数量
 * 
 */
@Mapper
public interface VideoPendingMapper {
    void updateVideoPending(VideoPending video);

    void addVideoPending(Video video);

    void deleteVideoPending(Long id);

    List<VideoPending> getVideoPendingByPage(VideoPendingDTO videoPendingDTO);

    VideoPending getVideoPendingById(Long id);

    List<VideoPending> getVideoPendingByStatus(Long userId, Integer status);

    Long getVideoCountByStatus(Integer status);

    void addVideoPendingWhichNoPass(VideoPending videoPending);

    Integer getVideoUndoReport(LocalDateTime beginDateTime, LocalDateTime endDateTime);
}

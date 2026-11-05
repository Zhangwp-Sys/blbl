package com.yilena.service.service.impl;

import com.google.protobuf.MapEntry;
import com.yilena.service.constant.EntityConstant;
import com.yilena.service.constant.MqConstant;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.dao.HistoryMapper;
import com.yilena.service.dao.UserMapper;
import com.yilena.service.dao.VideoMapper;
import com.yilena.service.entity.dto.HistoryDTO;
import com.yilena.service.entity.po.History;
import com.yilena.service.entity.po.User;
import com.yilena.service.entity.po.Video;
import com.yilena.service.entity.vo.VideoVO;
import com.yilena.service.service.HistoryService;
import com.yilena.service.utils.SnowFlake;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 历史记录服务实现类
 * 
 * 功能说明：
 * 1. 实现浏览历史记录相关的业务逻辑
 * 2. 处理历史记录的添加、查询、删除
 * 3. 实现历史记录开关功能
 * 
 * 核心功能：
 * - 历史记录添加：记录用户浏览的视频
 * - 历史记录查询：按日期分组查询历史记录
 * - 历史记录管理：删除单条或全部历史记录
 * - 开关功能：用户可控制是否记录浏览历史
 * 
 * 技术架构：
 * - 使用RabbitMQ异步存储历史记录
 * - 使用Map按日期分组存储历史记录
 * - 按日期降序排列历史记录
 * 
 */
@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    /**
     * RabbitMQ消息模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * 历史记录数据访问对象
     */
    private final HistoryMapper historyMapper;

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 添加历史记录
     * 
     * 功能：记录用户浏览的视频
     * 业务逻辑：
     * 1. 检查用户是否开启历史记录功能
     * 2. 查询视频信息
     * 3. 通过RabbitMQ异步存储历史记录
     * 
     * @param userId 用户ID
     * @param videoId 视频ID
     */
    @Override
    public void addHistory(Long userId, Long videoId) {
        User user = userMapper.getUserById(userId);
        if(user.getIsRecord().equals(StatusConstant.STATUS_YES)) {
            Video video = videoMapper.getVideoById(videoId);
            History history = new History();
            history.setId(snowFlake.getID());
            history.setEntityType(EntityConstant.VIDEO_TYPE);
            history.setEntityId(video.getId());
            history.setUserId(userId);
            history.setCreatedTime(LocalDateTime.now());
            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.HISTORY_BINDING_KEY, history);
        }
    }

    /**
     * 获取历史记录
     * 
     * 功能：获取用户的历史浏览记录
     * 业务逻辑：
     * 1. 查询用户的历史记录
     * 2. 获取视频详细信息
     * 3. 按日期分组
     * 4. 按日期降序排列
     * 
     * @param userId 用户ID
     * @return 按日期分组的历史记录（日期 -> 历史记录DTO）
     */
    @Override
    public Map<String, HistoryDTO> getHistory(Long userId) {
        Map<String, HistoryDTO> history =  historyMapper.getHistory(userId);

        for (Map.Entry<String, HistoryDTO> entry : history.entrySet()) {
            HistoryDTO historyDTO = entry.getValue();
            List<Long> entityIds = historyDTO.getEntityIds();
            List<VideoVO> videoVOS = videoMapper.getVideoByIds(entityIds);
            videoVOS.forEach(videoVO -> {
                Long videoUserId = videoVO.getUserId();
                videoVO.setUsername(userMapper.getUserById(videoUserId).getUsername());
            });

            // 进行重新排序
            Map<Long, VideoVO> videoMap = videoVOS.stream()
                    .collect(Collectors.toMap(VideoVO::getId, Function.identity()));
            List<VideoVO> sortedVideos = entityIds.stream()
                    .map(videoMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            historyDTO.setVideoVOS(sortedVideos);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        history =  history.entrySet().stream()
                .sorted((e1, e2) -> {
                    LocalDate d1 = LocalDate.parse(e1.getKey(), formatter);
                    LocalDate d2 = LocalDate.parse(e2.getKey(), formatter);
                    return d2.compareTo(d1); // 降序
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, // 碰撞策略（可选）
                        LinkedHashMap::new // 有序的 Map
                ));
        return history;
    }

    /**
     * 更新历史记录开关
     * 
     * 功能：更新用户历史记录记录开关状态
     * 
     * @param userId 用户ID
     * @param isRecord 开关状态（0-关闭，1-开启）
     */
    @Override
    public void updateIsRecord(Long userId, Integer isRecord) {
        userMapper.updateIsRecord(userId, isRecord);
    }

    /**
     * 删除全部历史记录
     * 
     * 功能：删除用户的全部历史记录
     * 
     * @param userId 用户ID
     */
    @Override
    public void deleteAllHistory(Long userId) {
        historyMapper.deleteAllHistory(userId);
    }

    /**
     * 删除单条历史记录
     * 
     * 功能：删除指定的历史记录
     * 
     * @param id 历史记录ID
     */
    @Override
    public void deleteHistory(Long id) {
        historyMapper.deleteHistory(id);
    }
}

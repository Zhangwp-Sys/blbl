package com.yilena.service.service.impl;

import com.yilena.service.constant.StatusConstant;
import com.yilena.service.dao.*;
import com.yilena.service.service.ManagerReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 管理员数据报表服务实现类
 * 
 * 功能说明：
 * 1. 实现管理员数据统计报表功能
 * 2. 支持7天、30天、90天的数据统计
 * 3. 统计用户数、视频数、动态数、评论数、点赞数、收藏数
 * 
 * 核心功能：
 * - 用户统计：统计用户总数和增长趋势
 * - 视频统计：统计视频状态和上传趋势
 * - 动态统计：统计动态上传趋势
 * - 内容统计：统计评论、点赞、收藏总数
 * 
 */
@RequiredArgsConstructor
@Service
public class ManagerReportServiceImpl implements ManagerReportService {

    /**
     * 待审核视频数据访问对象
     */
    private final VideoPendingMapper videoPendingMapper;

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 点赞数据访问对象
     */
    private final LikeMapper likeMapper;

    /**
     * 动态数据访问对象
     */
    private final PostMapper postMapper;

    /**
     * 评论数据访问对象
     */
    private final CommentMapper commentMapper;

    /**
     * 视频收藏数据访问对象
     */
    private final VideoFavoritesMapper videoFavoritesMapper;

    /**
     * 获取视频状态统计报表
     * 
     * 功能：统计各状态下的视频数量
     * 业务逻辑：
     * 1. 统计审核不通过的视频数
     * 2. 统计待审核的视频数
     * 3. 统计已发布的视频数
     * 4. 计算总数
     * 
     * @return 视频状态统计Map（no=不通过, wait=待审核, yes=已发布, total=总数）
     */
    @Override
    public Map<String, Long> getVideoReportStatus() {
        Map<String, Long> videoCount = new HashMap<>();
        videoCount.put("no", videoPendingMapper.getVideoCountByStatus(StatusConstant.STATUS_NO));
        videoCount.put("wait",  videoPendingMapper.getVideoCountByStatus(StatusConstant.STATUS_WAIT));
        videoCount.put("yes",videoMapper.getVideoCount());
        Long total = videoCount.get("no") + videoCount.get("wait") + videoCount.get("yes");
        videoCount.put("total", total);
        return videoCount;
    }

    /**
     * 获取用户总数
     * 
     * @return 用户总数
     */
    @Override
    public Long getTotalUser() {
        return userMapper.getTotalUser();
    }

    /**
     * 获取点赞总数
     * 
     * @return 点赞总数
     */
    @Override
    public Long getTotalLike() {
        return likeMapper.getTotalLike();
    }

    /**
     * 获取动态总数
     * 
     * @return 动态总数
     */
    @Override
    public Long getTotalPost() {
        return postMapper.getTotalPost();
    }

    /**
     * 获取评论总数
     * 
     * @return 评论总数
     */
    @Override
    public Long getTotalComment() {
        return commentMapper.getTotalComment();
    }

    /**
     * 获取收藏总数
     * 
     * @return 收藏总数
     */
    @Override
    public Long getTotalFavorite() {
        return videoFavoritesMapper.getTotalFavorite();
    }

    /**
     * 时间类型与天数的映射
     * 1=7天, 2=30天, 3=90天
     */
    private static final Map<Integer, Integer> TYPE_DAYS = Map.of(
            1, 7,
            2, 30,
            3, 90
    );

    /**
     * 生成日期分割点
     * 
     * 功能：将指定时间段均匀分为7段
     * 
     * @param startDate 开始日期
     * @param totalDays 总天数
     * @return 日期分割点列表
     */
    private List<LocalDateTime> generateDatePoints(LocalDateTime startDate, int totalDays) {
        int interval = (int) Math.ceil((double) totalDays / 7);
        List<LocalDateTime> points = new ArrayList<>(List.of(startDate));

        IntStream.range(1, 8)
                .forEach(i -> points.add(points.get(i-1).plusDays(interval)));

        // 确保最后一个点不超过今天
        points.replaceAll(date -> date.isAfter(LocalDateTime.now()) ? LocalDateTime.now() : date);
        return points.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 获取视频上传统计报表
     * 
     * 功能：统计指定时间段内的视频上传趋势
     * 
     * @param type 时间类型（1=7天, 2=30天, 3=90天）
     * @return 视频上传统计数据列表
     * @throws RuntimeException 时间类型无效时抛出异常
     */
    @Override
    public List<Map<String, Object>> getVideoUploadReport(Integer type) {
        if (!TYPE_DAYS.containsKey(type)) throw new RuntimeException("数据错误！");

        int totalDays = TYPE_DAYS.get(type);
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(totalDays - 1);

        // 生成7个时间段的日期分割点
        List<LocalDateTime> datePoints = generateDatePoints(startDate, totalDays);

        return IntStream.range(0, datePoints.size() - 1)
                .mapToObj(i -> {
                    LocalDateTime periodStart = datePoints.get(i);
                    LocalDateTime periodEnd = datePoints.get(i + 1).minusDays(1);
                    Integer count = getVideoUploadReportCount(periodStart, periodEnd);
                    return Map.<String, Object>of(
                            "date", periodEnd,
                            "count", Optional.ofNullable(count).orElse(0)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取动态上传统计报表
     * 
     * 功能：统计指定时间段内的动态上传趋势
     * 
     * @param type 时间类型（1=7天, 2=30天, 3=90天）
     * @return 动态上传统计数据列表
     * @throws RuntimeException 时间类型无效时抛出异常
     */
    @Override
    public List<Map<String, Object>> getPostUploadReport(Integer type) {
        if (!TYPE_DAYS.containsKey(type)) throw new RuntimeException("数据错误！");

        int totalDays = TYPE_DAYS.get(type);
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(totalDays - 1);

        // 生成7个时间段的日期分割点
        List<LocalDateTime> datePoints = generateDatePoints(startDate, totalDays);

        return IntStream.range(0, datePoints.size() - 1)
                .mapToObj(i -> {
                    LocalDateTime periodStart = datePoints.get(i);
                    LocalDateTime periodEnd = datePoints.get(i + 1).minusDays(1);
                    Integer count = getPostUploadReportCount(periodStart, periodEnd);
                    return Map.<String, Object>of(
                            "date", periodEnd,
                            "count", Optional.ofNullable(count).orElse(0)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取用户注册统计报表
     * 
     * 功能：统计指定时间段内的用户注册趋势
     * 
     * @param type 时间类型（1=7天, 2=30天, 3=90天）
     * @return 用户注册统计数据列表
     * @throws RuntimeException 时间类型无效时抛出异常
     */
    @Override
    public List<Map<String, Object>> getUserReport(Integer type) {
        if (!TYPE_DAYS.containsKey(type)) throw new RuntimeException("数据错误！");

        int totalDays = TYPE_DAYS.get(type);
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(totalDays - 1);

        // 生成7个时间段的日期分割点
        List<LocalDateTime> datePoints = generateDatePoints(startDate, totalDays);

        return IntStream.range(0, datePoints.size() - 1)
                .mapToObj(i -> {
                    LocalDateTime periodStart = datePoints.get(i);
                    LocalDateTime periodEnd = datePoints.get(i + 1).minusDays(1);
                    Integer count = getUserReportCount(periodStart, periodEnd);
                    return Map.<String, Object>of(
                            "date", periodEnd,
                            "count", Optional.ofNullable(count).orElse(0)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取视频审核统计报表
     * 
     * 功能：统计指定时间段内的视频审核趋势
     * 
     * @param type 时间类型（1=7天, 2=30天, 3=90天）
     * @return 视频审核统计数据列表
     * @throws RuntimeException 时间类型无效时抛出异常
     */
    @Override
    public List<Map<String, Object>> getVideoUndoReport(Integer type) {
        if (!TYPE_DAYS.containsKey(type)) throw new RuntimeException("数据错误！");

        int totalDays = TYPE_DAYS.get(type);
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(totalDays - 1);

        // 生成7个时间段的日期分割点
        List<LocalDateTime> datePoints = generateDatePoints(startDate, totalDays);

        return IntStream.range(0, datePoints.size() - 1)
                .mapToObj(i -> {
                    LocalDateTime periodStart = datePoints.get(i);
                    LocalDateTime periodEnd = datePoints.get(i + 1).minusDays(1);
                    Integer count = getVideoUndoReportCount(periodStart, periodEnd);
                    return Map.<String, Object>of(
                            "date", periodEnd,
                            "count", Optional.ofNullable(count).orElse(0)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取指定时间段内的视频上传数
     */
    private Integer getVideoUploadReportCount(LocalDateTime start, LocalDateTime end) {
        return videoMapper.getVideoUploadReport(
                start.withHour(0).withMinute(0).withSecond(0).withNano(0) ,
                end.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)   // 包含结束日期的数据
        );
    }

    /**
     * 获取指定时间段内的动态上传数
     */
    private Integer getPostUploadReportCount(LocalDateTime start, LocalDateTime end) {
        return postMapper.getPostUploadReport(
                start.withHour(0).withMinute(0).withSecond(0).withNano(0) ,
                end.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)   // 包含结束日期的数据
        );
    }

    /**
     * 获取指定时间段内的用户注册数
     */
    private Integer getUserReportCount(LocalDateTime start, LocalDateTime end) {
        return userMapper.getUserReport(
                start.withHour(0).withMinute(0).withSecond(0).withNano(0) ,
                end.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)   // 包含结束日期的数据
        );
    }

    /**
     * 获取指定时间段内的视频审核数
     */
    private Integer getVideoUndoReportCount(LocalDateTime start, LocalDateTime end) {
        return videoPendingMapper.getVideoUndoReport(
                start.withHour(0).withMinute(0).withSecond(0).withNano(0) ,
                end.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)   // 包含结束日期的数据
        );
    }
}

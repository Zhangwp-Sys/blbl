package com.yilena.service.service.impl;

import com.yilena.service.dao.*;
import com.yilena.service.service.UserReportService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 用户数据报表服务实现类
 * 
 * 功能说明：
 * 1. 实现用户数据统计报表功能
 * 2. 支持7天、30天、90天的数据统计
 * 3. 统计关注数、点赞数、收藏数、投币数、评论数
 * 
 * 核心功能：
 * - 关注统计：统计指定时间段内的关注增长
 * - 点赞统计：统计指定时间段内的点赞增长
 * - 收藏统计：统计指定时间段内的收藏增长
 * - 投币统计：统计指定时间段内的投币增长
 * - 评论统计：统计指定时间段内的评论增长
 * 
 * 技术特点：
 * - 使用Stream API进行数据流处理
 * - 使用Map存储统计数据
 * - 支持时间分段统计
 * 
 */
@Service
@AllArgsConstructor
public class UserReportServiceImpl implements UserReportService {

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

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
     * 获取用户关注者统计报表
     * 
     * 功能：统计指定时间段内的关注增长趋势
     * 
     * @param userId 用户ID
     * @param type 时间类型（1=7天, 2=30天, 3=90天）
     * @return 关注统计数据列表
     * @throws RuntimeException 时间类型无效时抛出异常
     */
    public List<Map<String, Object>> getUserReportFollowers(Long userId, Integer type) {
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
                    Integer count = getPeriodFollowersCount(userId, periodStart, periodEnd);
                    return Map.<String, Object>of(
                            "date", periodEnd,
                            "count", Optional.ofNullable(count).orElse(0)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取用户点赞统计报表
     * 
     * 功能：统计指定时间段内的点赞增长趋势
     * 
     * @param userId 用户ID
     * @param type 时间类型（1=7天, 2=30天, 3=90天）
     * @return 点赞统计数据列表
     * @throws RuntimeException 时间类型无效时抛出异常
     */
    @Override
    public List<Map<String, Object>> getUserReportLikes(Long userId, Integer type) {
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
                    Integer count = getPeriodLikesCount(userId, periodStart, periodEnd);
                    return Map.<String, Object>of(
                            "date", periodEnd,
                            "count", Optional.ofNullable(count).orElse(0)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取用户收藏统计报表
     * 
     * 功能：统计指定时间段内的收藏增长趋势
     * 
     * @param userId 用户ID
     * @param type 时间类型（1=7天, 2=30天, 3=90天）
     * @return 收藏统计数据列表
     * @throws RuntimeException 时间类型无效时抛出异常
     */
    @Override
    public List<Map<String, Object>> getUserReportFavorites(Long userId, Integer type) {
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
                    Integer count = getPeriodFavoritesCount(userId, periodStart, periodEnd);
                    return Map.<String, Object>of(
                            "date", periodEnd,
                            "count", Optional.ofNullable(count).orElse(0)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取用户投币统计报表
     * 
     * 功能：统计指定时间段内的投币增长趋势
     * 
     * @param userId 用户ID
     * @param type 时间类型（1=7天, 2=30天, 3=90天）
     * @return 投币统计数据列表
     * @throws RuntimeException 时间类型无效时抛出异常
     */
    @Override
    public List<Map<String, Object>> getUserReportCoins(Long userId, Integer type) {
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
                    Integer count = getPeriodCoinsCount(userId, periodStart, periodEnd);
                    return Map.<String, Object>of(
                            "date", periodEnd,
                            "count", Optional.ofNullable(count).orElse(0)
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取用户评论统计报表
     * 
     * 功能：统计指定时间段内的评论增长趋势
     * 
     * @param userId 用户ID
     * @param type 时间类型（1=7天, 2=30天, 3=90天）
     * @return 评论统计数据列表
     * @throws RuntimeException 时间类型无效时抛出异常
     */
    @Override
    public List<Map<String, Object>> getUserReportComments(Long userId, Integer type) {
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
                    Integer count = getPeriodCommentsCount(userId, periodStart, periodEnd);
                    return Map.<String, Object>of(
                            "date", periodEnd,
                            "count", Optional.ofNullable(count).orElse(0)
                    );
                })
                .collect(Collectors.toList());
    }

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
     * 获取指定时间段内的关注数
     */
    private Integer getPeriodFollowersCount(Long userId, LocalDateTime start, LocalDateTime end) {
        return videoMapper.getFollowingFromReportCount(
                userId,
                start.withHour(0).withMinute(0).withSecond(0).withNano(0) ,
                end.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)   // 包含结束日期的数据
        );
    }

    /**
     * 获取指定时间段内的点赞数
     */
    private Integer getPeriodLikesCount(Long userId, LocalDateTime start, LocalDateTime end) {
        return videoMapper.getLikeFromReportCount(
                userId,
                start.withHour(0).withMinute(0).withSecond(0).withNano(0) ,
                end.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)   // 包含结束日期的数据
        );
    }

    /**
     * 获取指定时间段内的收藏数
     */
    private Integer getPeriodFavoritesCount(Long userId, LocalDateTime start, LocalDateTime end) {
        return videoMapper.getFavoriteFromReportCount(
                userId,
                start.withHour(0).withMinute(0).withSecond(0).withNano(0) ,
                end.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)   // 包含结束日期的数据
        );
    }

    /**
     * 获取指定时间段内的投币数
     */
    private Integer getPeriodCoinsCount(Long userId, LocalDateTime start, LocalDateTime end) {
        return videoMapper.getCoinFromReportCount(
                userId,
                start.withHour(0).withMinute(0).withSecond(0).withNano(0) ,
                end.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)   // 包含结束日期的数据
        );
    }

    /**
     * 获取指定时间段内的评论数
     */
    private Integer getPeriodCommentsCount(Long userId, LocalDateTime start, LocalDateTime end) {
        return videoMapper.getCommentFromReportCount(
                userId,
                start.withHour(0).withMinute(0).withSecond(0).withNano(0) ,
                end.plusDays(1).withHour(0).withMinute(0).withSecond(0).withNano(0)   // 包含结束日期的数据
        );
    }
}

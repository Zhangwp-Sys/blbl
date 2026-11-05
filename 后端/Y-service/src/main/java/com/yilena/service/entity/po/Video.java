package com.yilena.service.entity.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 视频实体类
 * 
 * 功能说明：
 * 1. 存储视频基本信息
 * 2. 映射数据库视频表
 * 3. 提供视频数据访问对象
 * 
 * 字段说明：
 * - id: 视频ID（主键）
 * - lastId: 上一个视频ID（用于链表结构）
 * - title: 视频标题
 * - description: 视频描述
 * - videoUrl: 视频文件URL
 * - coverUrl: 视频封面URL
 * - tags: 视频标签列表
 * - tagsJson: 标签JSON字符串
 * - sort: 排序权重
 * - barrages: 弹幕数
 * - views: 播放量
 * - likes: 点赞数
 * - favorites: 收藏数
 * - shares: 分享数
 * - coins: 硬币数
 * - comments: 评论数
 * - userId: 上传用户ID
 * - collectionId: 所属收藏夹ID
 * - collectionName: 收藏夹名称
 * - visibility: 可见性（0:公开，1:私密）
 * - time: 视频时长（毫秒）
 * - createdTime: 创建时间
 * - updatedTime: 更新时间
 * 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video implements Serializable {

    /**
     * 视频ID（主键）
     */
    private Long id;

    /**
     * 上一个视频ID（用于链表结构）
     */
    private Long lastId;

    /**
     * 视频标题
     */
    private String title;

    /**
     * 视频描述
     */
    private String description;

    /**
     * 视频文件URL
     */
    private String videoUrl;

    /**
     * 视频封面URL
     */
    private String coverUrl;

    /**
     * 视频标签列表
     */
    private List<String> tags; // JSON转换

    /**
     * 标签JSON字符串
     */
    private String tagsJson;

    /**
     * 排序权重
     */
    private Integer sort;

    /**
     * 弹幕数
     */
    private Integer barrages;

    /**
     * 播放量
     */
    private Integer views;

    /**
     * 点赞数
     */
    private Integer likes;

    /**
     * 收藏数
     */
    private Integer favorites;

    /**
     * 分享数
     */
    private Integer shares;

    /**
     * 硬币数
     */
    private Integer coins;

    /**
     * 评论数
     */
    private Integer comments;

    /**
     * 上传用户ID
     */
    private Long userId;

    /**
     * 所属收藏夹ID
     */
    private Long collectionId;

    /**
     * 收藏夹名称
     */
    private String collectionName;

    /**
     * 可见性（0:公开，1:私密）
     */
    private Integer visibility;

    /**
     * 视频时长（毫秒）
     */
    private Long time;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
}
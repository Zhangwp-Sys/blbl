package com.yilena.service.entity.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子实体类
 * 
 * 功能说明：
 * 1. 存储帖子基本信息
 * 2. 映射数据库帖子表
 * 3. 提供帖子数据访问对象
 * 
 * 字段说明：
 * - id: 帖子ID（主键）
 * - title: 帖子标题
 * - content: 帖子内容
 * - imageUrl: 图片URL列表
 * - imageUrlJson: 图片URL JSON字符串
 * - tags: 标签列表
 * - tagsJson: 标签JSON字符串
 * - videoId: 关联视频ID
 * - clicks: 点击数
 * - likes: 点赞数
 * - favorites: 收藏数
 * - shares: 分享数
 * - comments: 评论数
 * - userId: 发帖用户ID
 * - visibility: 可见性（0:公开，1:私密）
 * - createdTime: 创建时间
 * - updatedTime: 更新时间
 * - PostType: 帖子类型（0:普通帖子，1:视频帖子）
 * 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post implements Serializable {

    /**
     * 帖子ID（主键）
     */
    private Long id;

    /**
     * 帖子标题
     */
    private String title;

    /**
     * 帖子内容
     */
    private String content;

    /**
     * 图片URL列表
     */
    private List<String> imageUrl;

    /**
     * 图片URL JSON字符串
     */
    private String imageUrlJson;

    /**
     * 标签列表
     */
    private List<String> tags;

    /**
     * 标签JSON字符串
     */
    private String tagsJson;

    /**
     * 关联视频ID
     */
    private Long videoId;

    /**
     * 点击数
     */
    private Integer clicks;

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
     * 评论数
     */
    private Integer comments;

    /**
     * 发帖用户ID
     */
    private Long userId;

    /**
     * 可见性（0:公开，1:私密）
     */
    private Integer visibility;

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

    /**
     * 帖子类型（0:普通帖子，1:视频帖子）
     */
    private Integer PostType;
}
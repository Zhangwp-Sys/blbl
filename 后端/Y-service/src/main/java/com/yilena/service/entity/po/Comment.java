package com.yilena.service.entity.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论实体类
 * 
 * 功能说明：
 * 1. 存储评论基本信息
 * 2. 映射数据库评论表
 * 3. 支持多类型实体评论
 * 
 * 字段说明：
 * - id: 评论ID（主键）
 * - entityType: 实体类型（0:视频，1:帖子）
 * - entityId: 实体ID
 * - userId: 评论用户ID
 * - content: 评论内容
 * - imageUrl: 评论图片URL
 * - likes: 点赞数
 * - comments: 回复数
 * - createdTime: 创建时间
 * - updatedTime: 更新时间
 * - videoTitle: 视频标题（关联查询）
 * - username: 用户名（关联查询）
 * - type: 评论类型（0:普通评论，1:热门评论）
 * 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment  implements Serializable {

    /**
     * 评论ID（主键）
     */
    private Long id;

    /**
     * 实体类型（0:视频，1:帖子）
     */
    private Integer entityType;

    /**
     * 实体ID
     */
    private Long entityId;

    /**
     * 评论用户ID
     */
    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论图片URL
     */
    private String imageUrl;

    /**
     * 点赞数
     */
    private Integer likes;

    /**
     * 回复数
     */
    private Integer comments;

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
     * 视频标题（关联查询）
     */
    private String videoTitle;

    /**
     * 用户名（关联查询）
     */
    private String username;

    /**
     * 评论类型（0:普通评论，1:热门评论）
     */
    private Integer type;
}
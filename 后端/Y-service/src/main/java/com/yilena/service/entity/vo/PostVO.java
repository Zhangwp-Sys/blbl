package com.yilena.service.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态视图对象
 * 
 * 功能说明：
 * 1. 封装动态完整信息返回给前端
 * 2. 包含动态基本信息、统计数据和关联视频信息
 * 
 * 字段说明：
 * - id: 动态ID
 * - title: 动态标题
 * - content: 动态内容
 * - imageUrl: 图片URL列表
 * - tags: 标签列表
 * - clicks: 浏览量
 * - likes: 点赞数
 * - favorites: 收藏数
 * - shares: 分享数
 * - comments: 评论数
 * - userId: 用户ID
 * - username: 用户名
 * - video*字段: 关联视频信息
 * 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostVO implements Serializable {
    private Long id;
    private String title;
    private String content;
    private List<String> imageUrl;
    private String imageUrlJson;
    private List<String> tags;
    private Long videoId;
    private String tagsJson;
    private Integer clicks;
    private Integer likes;
    private Integer favorites;
    private Integer shares;
    private Integer comments;
    private Long userId;
    private String username;
    private String image;
    private Integer visibility;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;
    // 唯一标识
    private String symbol;
    private Integer PostType;
    private Integer type;

    private String videoUsername;
    private Long videoUserId;
    private String videoUserImage;
    private String videoCoverUrl;
    private String videoTitle;
    private String videoDescription;
    private Integer videoViews;
    private Integer videoBarrages;
    private double videoTime;
}
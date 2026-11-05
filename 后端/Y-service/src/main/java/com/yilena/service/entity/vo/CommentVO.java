package com.yilena.service.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论视图对象
 * 
 * 功能说明：
 * 1. 封装评论完整信息返回给前端
 * 2. 包含评论内容、用户信息和统计数据
 * 
 * 字段说明：
 * - id: 评论ID
 * - userId: 用户ID
 * - username: 用户名
 * - image: 用户头像
 * - content: 评论内容
 * - imageUrl: 评论图片URL
 * - likes: 点赞数
 * - comments: 回复数
 * - createdTime: 创建时间
 * - updatedTime: 更新时间
 * 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO  implements Serializable {
    private Long id;
    private Long userId;
    private String username;
    private String image;
    private String content;
    private String imageUrl;
    private Integer likes;
    // 回复
    private Integer comments;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;

}
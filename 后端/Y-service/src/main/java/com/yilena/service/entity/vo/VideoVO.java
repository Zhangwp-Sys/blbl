package com.yilena.service.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 视频视图对象
 * 
 * 功能说明：
 * 1. 封装视频完整信息返回给前端
 * 2. 包含视频基本信息、统计数据和用户信息
 * 
 * 字段说明：
 * - id: 视频ID
 * - title: 视频标题
 * - description: 视频描述
 * - coverUrl: 封面URL
 * - tags: 视频标签列表
 * - views: 浏览量
 * - barrages: 弹幕数
 * - favorites: 收藏数
 * - userId: 用户ID
 * - username: 用户名
 * - createdTime: 创建时间
 * 
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoVO implements Serializable {
    private Long id;

    private Long lastId;

    private String title;

    private String description;

    private String coverUrl;

    private List<String> tags;
    private Integer sort;
    private Integer barrages;
    private Integer views;
    private Integer visibility;
    private Integer favorites;

    private Long time;

    private Long userId;

    private String username;

    private LocalDateTime createdTime;

    private Integer type;
}
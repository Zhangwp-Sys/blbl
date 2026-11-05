package com.yilena.service.constant;

/**
 * 实体类型常量类
 * 
 * 功能说明：
 * 1. 定义实体类型常量（视频/动态/评论）
 * 2. 定义排序方式常量
 * 
 * 实体类型：
 * - VIDEO_TYPE = 1: 视频
 * - POST_TYPE = 2: 动态
 * - COMMENT_TYPE = 3: 评论
 * 
 * 排序方式：
 * - HOT_TYPE = 1: 热门
 * - NEW_TYPE = 2: 最新
 * - VIDEO_NEW_TYPE = 1: 视频最新
 * - VIDEO_VIEWS_TYPE = 2: 按播放量
 * - VIDEO_FAVORITES_TYPE = 3: 按收藏数
 * 
 */
public class EntityConstant {
    public final static Integer VIDEO_TYPE = 1;
    public final static Integer POST_TYPE = 2;
    public final static Integer COMMENT_TYPE = 3;
    public final static Integer HOT_TYPE = 1;
    public final static Integer NEW_TYPE = 2;

    public final static Integer COMMENT_NEW_TYPE = 1;
    public final static Integer COMMENT_LIKE_TYPE = 2;
    public final static Integer COMMENT_MOST_COMMENTS_TYPE = 3;

    public final static Integer VIDEO_NEW_TYPE = 1;
    public final static Integer VIDEO_VIEWS_TYPE = 2;
    public final static Integer VIDEO_FAVORITES_TYPE = 3;
}

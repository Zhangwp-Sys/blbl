package com.yilena.service.constant;

/**
 * 消息类型常量类
 * 
 * 功能说明：
 * 1. 定义站内消息类型常量
 * 2. 用于区分不同种类的通知消息
 * 
 * 消息类型：
 * - TEXT = 1: 文本消息
 * - IMAGE = 2: 图片消息
 * - LIKE = 3: 点赞通知
 * - FOLLOW = 4: 关注通知
 * - COMMENT = 5: 评论通知
 * - MANAGER = 6: 管理员通知
 * - FAVORITES = 7: 收藏通知
 * - VIDEO = 8: 视频通知
 * - At = 9: @提及通知
 * 
 */
public class MessageTypeConstant {
    public static final Integer TEXT = 1;
    public static final Integer IMAGE = 2;
    public static final Integer LIKE = 3;
    public static final Integer FOLLOW = 4;
    public static final Integer COMMENT = 5;
    public static final Integer MANAGER = 6;
    public static final Integer FAVORITES = 7;
    public static final Integer VIDEO = 8;
    public static final Integer At = 9;
}

package com.yilena.service.constant;

/**
 * 系统状态常量类
 * 
 * 功能说明：
 * 1. 定义系统状态常量（启用/禁用/待审核）
 * 2. 定义资源不存在时的错误消息常量
 * 
 * 常量说明：
 * - STATUS_YES = 1: 启用/有效/通过
 * - STATUS_NO = 0: 禁用/无效/不通过
 * - STATUS_WAIT = 2: 待审核
 * 
 */
public class StatusConstant {
    public static final Integer STATUS_YES = 1;
    public static final Integer STATUS_NO = 0;
    public static final Integer STATUS_WAIT = 2;
    public static final String VIDEO_COLLECTION_NOT_EXIST = "投稿合集不存在！";
    public static final String VIDEO_NOT_EXIST = "视频不存在！";
    public static final String POST_NOT_EXIST = "动态不存在！";
    public static final String COMMENT_NOT_EXIST = "评论不存在！";
    public static final String VIDEO_ALREADY_COLLECTION = "视频已存在！";
}

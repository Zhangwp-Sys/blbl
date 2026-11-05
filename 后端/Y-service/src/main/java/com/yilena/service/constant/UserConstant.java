package com.yilena.service.constant;

/**
 * 用户提示消息常量类
 * 
 * 功能说明：
 * 1. 定义用户登录相关提示消息
 * 2. 定义用户注册相关提示消息
 * 3. 定义用户状态相关提示消息
 * 
 */
public class UserConstant {
    public final static String USER_EMPTY_FAIL = "用户名或密码不能为空！";
    public final static String USER_LOGIN_FAIL = "用户名或密码错误！";
    public final static String USER_LOGIN_SUCCESS = "登录成功！";
    public final static String USER_LOGIN_INEXIST_SUCCESS = "用户已存在！";
    public final static String USER_LOGIN_INEXIST_FAIL = "用户不存在！";
    public final static String USER_REGISTER_SUCCESS = "注册成功！";
    public final static String USER_REGISTER_FAIL = "注册失败！";

    public static final String USER_CAN_NOT_LOGIN = "用户已被冻结！无法登录！";
    public static final String USER_CAN_NOT_DO_ANYTHING = "用户已被冻结！";
    public static final String USER_PASSWORD_WRONG = "原密码错误！请重新输入！";
    public static final String USER_PASSWORD_SAME_FAIL = "两次密码一致！";
}

package com.yilena.service.utils;

/**
 * 当前用户持有者工具类
 * 
 * 功能说明：
 * 1. 使用ThreadLocal存储当前登录用户ID
 * 2. 提供设置、获取和清除当前用户的方法
 * 3. 确保每个线程独立的用户上下文
 * 
 * 使用场景：
 * - 在请求处理过程中存储当前登录用户ID
 * - 在业务逻辑中获取当前用户信息
 * - 请求结束后清理用户上下文，防止内存泄漏
 * 
 * 技术实现：
 * - 使用ThreadLocal实现线程安全的用户上下文存储
 * - 每个请求线程都有独立的用户ID副本
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
public class CurrentHolder {

    /**
     * ThreadLocal存储当前用户ID
     */
    private static final ThreadLocal<Long> CURRENT_LOCAL = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     * 
     * 功能：将用户ID存储到当前线程的ThreadLocal中
     * 使用场景：用户登录成功后调用
     * 
     * @param userIdD 用户ID
     */
    public static void setCurrent(Long userIdD) {
        CURRENT_LOCAL.set(userIdD);
    }

    /**
     * 获取当前用户ID
     * 
     * 功能：从当前线程的ThreadLocal中获取用户ID
     * 使用场景：在业务逻辑中获取当前登录用户
     * 
     * @return 当前用户ID，如果未设置则返回null
     */
    public static Long getCurrent() {
        return CURRENT_LOCAL.get();
    }

    /**
     * 清除当前用户ID
     * 
     * 功能：清除当前线程ThreadLocal中的用户ID
     * 使用场景：请求结束后调用，防止内存泄漏
     * 
     */
    public static void removeCurrent() {
        CURRENT_LOCAL.remove();
    }
}
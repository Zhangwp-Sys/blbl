package com.yilena.service.entity.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户实体类
 * 
 * 功能说明：
 * 1. 存储用户基本信息
 * 2. 映射数据库用户表
 * 3. 提供用户数据访问对象
 * 
 * 字段说明：
 * - id: 用户ID（主键）
 * - username: 用户名（唯一）
 * - password: 密码（BCrypt加密）
 * - image: 用户头像URL
 * - description: 用户描述
 * - followingCount: 关注数
 * - followersCount: 粉丝数
 * - postCount: 帖子数
 * - videoCount: 视频数
 * - coins: 硬币数
 * - status: 用户状态（0:正常，1:禁用）
 * - reason: 状态原因
 * - isRecord: 是否记录历史（0:记录，1:不记录）
 * - createdTime: 创建时间
 * - updatedTime: 更新时间
 * 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements Serializable{

    /**
     * 用户ID（主键）
     */
    private Long id;

    /**
     * 用户名（唯一）
     */
    private String username;

    /**
     * 旧密码（用于密码修改验证）
     */
    private String oldPassword;

    /**
     * 密码（BCrypt加密存储）
     */
    private String password;

    /**
     * 用户头像URL
     */
    private String image;

    /**
     * 用户描述
     */
    private String description;

    /**
     * 关注数
     */
    private Integer followingCount;

    /**
     * 粉丝数
     */
    private Integer followersCount;

    /**
     * 帖子数
     */
    private Integer postCount;

    /**
     * 视频数
     */
    private Integer videoCount;

    /**
     * 硬币数
     */
    private Integer coins;

    /**
     * 用户状态（0:正常，1:禁用）
     */
    private Integer status;

    /**
     * 状态原因（禁用原因）
     */
    private String reason;

    /**
     * 是否记录历史（0:记录，1:不记录）
     */
    private Integer isRecord;

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
}
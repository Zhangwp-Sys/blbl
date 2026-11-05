package com.yilena.service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录信息实体类
 * 
 * 功能说明：
 * 1. 封装用户登录/注册返回信息
 * 2. 包含用户名、密码和JWT令牌
 * 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginInfo {
    private String username;
    private String password;
    private String token;
}

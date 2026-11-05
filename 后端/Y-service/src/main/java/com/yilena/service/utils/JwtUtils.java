package com.yilena.service.utils;

import com.aliyuncs.utils.StringUtils;
import com.yilena.service.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 
 * 功能说明：
 * 1. 生成JWT Token
 * 2. 解析JWT Token
 * 3. 验证Token有效性
 * 
 * JWT结构：
 * - Header: 包含算法和类型信息
 * - Payload: 包含用户信息和声明
 * - Signature: 包含签名信息
 * 
 * 使用场景：
 * - 用户登录成功后生成Token
 * - 请求拦截器中验证Token
 * - 获取Token中的用户信息
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;

    /**
     * JWT密钥
     */
    private static  String secretKey;

    /**
     * Token过期时间（毫秒）
     */
    private static  long expire;

    /**
     * 初始化静态变量
     * 
     * 功能：从配置文件中读取JWT配置
     * 执行时机：Bean初始化完成后自动执行
     * 
     */
    @PostConstruct
    public void initStatic() {
        secretKey = jwtProperties.getSecretKey();
        expire = jwtProperties.getExpire();
        if (StringUtils.isEmpty(secretKey)) {
            throw new IllegalArgumentException("JWT密钥不能为空");
        }
    }

    /**
     * 生成JWT Token
     * 
     * 功能：根据用户信息生成JWT Token
     * 业务逻辑：
     * 1. 将用户信息添加到Token中
     * 2. 使用密钥进行签名
     * 3. 设置过期时间
     * 
     * @param claims 用户信息和声明
     * @return JWT Token字符串
     */
    public static String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .addClaims(claims)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .setExpiration(new Date(System.currentTimeMillis() + expire))
                .compact();
    }

    /**
     * 解析JWT Token
     * 
     * 功能：解析JWT Token获取用户信息
     * 业务逻辑：
     * 1. 验证Token签名
     * 2. 检查Token是否过期
     * 3. 解析用户信息
     * 
     * @param jwt JWT Token字符串
     * @return Claims 用户信息和声明
     */
    public static Claims parseToken(String jwt) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(jwt)
                .getBody();
    }

    /**
     * 获取Token剩余有效时间（毫秒）
     *
     * 功能：计算Token距离过期还剩多少时间
     * 使用场景：登出时将Token加入Redis黑名单，TTL设置为剩余有效期，
     *          避免黑名单key永久占用内存
     *
     * @param token JWT Token字符串
     * @return 剩余有效毫秒数（已过期则返回负数）
     */
    public static long getRemainingMillis(String token) {
        Claims claims = parseToken(token);
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
}
package com.yilena.service.controller.user;

import com.yilena.service.constant.UserConstant;
import com.yilena.service.entity.Result;
import com.yilena.service.entity.LoginInfo;
import com.yilena.service.entity.dto.UserDTO;
import com.yilena.service.entity.po.User;
import com.yilena.service.log.LogOperation;
import com.yilena.service.service.UserService;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 用户控制器
 * 
 * 功能说明：
 * 1. 处理用户认证相关的HTTP请求
 * 2. 提供用户登录、注册、登出功能
 * 3. 支持用户信息查询和修改
 * 4. 提供用户分页查询功能
 * 
 * API接口：
 * - POST /user/login - 用户登录
 * - POST /user/register - 用户注册
 * - POST /user/logout - 用户登出
 * - GET /user/search/page - 分页查询用户
 * - GET /user/search/{id} - 根据ID查询用户
 * - GET /user/search/username/{username} - 根据用户名查询用户
 * - PUT /user - 更新用户信息
 * - GET /user/getToken/{id} - 获取用户Token
 * 
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 用户登录
     * 
     * 功能：验证用户身份并生成登录凭证
     * 业务逻辑：
     * 1. 接收用户名和密码
     * 2. 验证用户身份
     * 3. 生成JWT Token
     * 4. 返回登录信息
     * 
     * @param user 用户实体对象（包含用户名和密码）
     * @return Result 包含登录信息（Token等）
     */
    @PostMapping("/login")
    public Result login(@RequestBody User user){
        log.info("用户{}登录", user.getUsername());
        LoginInfo loginInfo =userService.login(user);
        // 登录失败
        if (loginInfo == null){
            return Result.error(UserConstant.USER_LOGIN_FAIL);
        }
        return Result.success(loginInfo);
    }

    /**
     * 用户注册
     * 
     * 功能：创建新用户账户
     * 业务逻辑：
     * 1. 接收用户注册信息
     * 2. 验证用户名唯一性
     * 3. 加密密码
     * 4. 创建用户记录
     * 
     * @param user 用户实体对象（包含用户名和密码）
     * @return Result 操作结果
     */
    @PostMapping("/register")
    public Result register(@RequestBody User user){
        log.info("用户{}注册", user.getUsername());
        userService.register(user);
        return Result.success();
    }

    /**
     * 用户登出
     * 
     * 功能：清除当前用户的登录状态
     * 业务逻辑：
     * 1. 将当前Token写入Redis黑名单（TTL为Token剩余有效期，到期自动清理）
     * 2. 清除ThreadLocal中的用户信息
     * 3. 返回登出结果
     *
     * @param token 请求头中的Token
     * @return Result 操作结果
     */
    @PostMapping("/logout")
    public Result logout(@RequestHeader("token") String token){
        log.info("用户{}退出登录", CurrentHolder.getCurrent());
        // Token加入黑名单，TTL为剩余有效期
        long remaining = JwtUtils.getRemainingMillis(token);
        if (remaining > 0) {
            redisTemplate.opsForValue().set("token:blacklist:" + token, "1", remaining, TimeUnit.MILLISECONDS);
        }
        CurrentHolder.removeCurrent();
        return Result.success();
    }

    /**
     * 分页查询用户
     * 
     * 功能：根据查询条件分页获取用户列表
     * 业务逻辑：
     * 1. 支持按用户名、状态等条件查询
     * 2. 支持分页功能
     * 3. 清除搜索历史缓存
     * 
     * @param userDTO 用户查询条件DTO
     * @return Result 包含分页用户列表
     */
    @GetMapping("/search/page")
    @CacheEvict(value = "searchHistory", key = "'page'")
    public Result getUserByPage(UserDTO userDTO){
        log.info("用户查询用户分页信息");
        return Result.success(userService.getUserByPage(userDTO));
    }

    /**
     * 根据ID查询用户
     * 
     * 功能：根据用户ID获取用户详细信息
     * 业务逻辑：
     * 1. 根据ID查询用户
     * 2. 使用Redis缓存提高查询性能
     * 3. 返回用户详细信息
     * 
     * @param id 用户ID
     * @return Result 包含用户详细信息
     */
    @Cacheable(value = "user", key = "#id", sync = true)
    @GetMapping("/search/{id}")
    public Result getUserById(@PathVariable Long id){
        log.info("用户查询用户id:{}信息", id);
        return Result.success(userService.getUserById(id));
    }

    /**
     * 根据用户名查询用户
     * 
     * 功能：根据用户名获取用户详细信息
     * 业务逻辑：
     * 1. 根据用户名查询用户
     * 2. 使用Redis缓存提高查询性能
     * 3. 返回用户详细信息
     * 
     * @param username 用户名
     * @return Result 包含用户详细信息
     */
    @Cacheable(value = "user", key = "#username", sync = true)
    @GetMapping("/search/username/{username}")
    public Result getUserByUsername(@PathVariable String username){
        log.info("用户查询用户名:{}信息", username);
        return Result.success(userService.getUserByUsername(username));
    }

    /**
     * 更新用户信息
     * 
     * 功能：修改用户的基本信息
     * 业务逻辑：
     * 1. 接收更新后的用户信息
     * 2. 记录操作日志
     * 3. 更新用户记录
     * 4. 清除相关缓存
     * 
     * @param user 用户实体对象
     * @return Result 操作结果
     */
    @Caching(evict = {
            @CacheEvict(value = "user", key = "#user.id"),
            @CacheEvict(value = "user", key = "#user.username")
    })
    @LogOperation
    @PutMapping
    public Result updateUserById(@RequestBody User user){
        log.info("用户修改用户id:{}信息",user.getId());
        userService.updateUserById(user);
        // 延迟双删：方法返回前注解已完成第一次删缓存+DB更新，
        // 延迟500ms再删一次，覆盖并发读请求把旧值回写缓存的时间窗口（网络传输+DB更新+缓存重建）
        CompletableFuture.runAsync(() -> {
            cacheManager.getCache("user").evict(user.getId());
            cacheManager.getCache("user").evict(user.getUsername());
            log.info("延迟双删执行：user缓存二次清除，id={}, username={}", user.getId(), user.getUsername());
        }, CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS));
        return Result.success();
    }

    /**
     * 获取用户Token
     * 
     * 功能：根据用户ID生成JWT Token
     * 业务逻辑：
     * 1. 根据用户ID获取用户信息
     * 2. 生成JWT Token
     * 3. 返回Token
     * 
     * @param id 用户ID
     * @return Result 包含JWT Token
     */
    @GetMapping("/getToken/{id}")
    public Result getToken(@PathVariable Long id){
        log.info("用户获取token");
        return Result.success(userService.getToken(id));
    }
}

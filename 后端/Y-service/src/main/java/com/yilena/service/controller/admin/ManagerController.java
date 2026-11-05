package com.yilena.service.controller.admin;

import com.yilena.service.constant.UserConstant;
import com.yilena.service.entity.LoginInfo;
import com.yilena.service.entity.Result;
import com.yilena.service.entity.dto.ManagerPageQueryDTO;
import com.yilena.service.entity.po.Manager;
import com.yilena.service.log.LogOperation;
import com.yilena.service.service.ManagerService;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

/**
 * 管理员控制器
 * 
 * 功能说明：
 * 1. 提供管理员登录功能
 * 2. 支持管理员信息的增删改查
 * 3. 支持管理员密码修改
 * 4. 支持分页查询管理员列表
 * 
 * 请求路径：/manager
 * 
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/manager")
public class ManagerController {

    private final ManagerService managerService;
    private final RedisTemplate<String, Object> redisTemplate;

    @PostMapping("/login")
    public Result login(@RequestBody Manager manager) {
        log.info("管理员登录");
        LoginInfo loginInfo = managerService.login(manager);
        if(loginInfo == null){
            return Result.error(UserConstant.USER_LOGIN_FAIL);
        }
        return Result.success(loginInfo);
    }

    @LogOperation
    @PutMapping("/status/{status}/{id}")
    public Result updateStatus(@PathVariable Integer status, @PathVariable Integer id) {
        log.info("管理员修改状态");
        managerService.updateStatus(status, id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getManagerById(@PathVariable Integer id) {
        log.info("管理员查询");
        Manager manager = managerService.getManagerById(id);
        return Result.success(manager);
    }

    @LogOperation
    @PutMapping("/password")
    public Result updatePassword(@RequestBody Manager manager) {
        log.info("管理员修改密码");
        managerService.updatePassword(manager);
        return Result.success();
    }

    @PostMapping("/logout")
    public Result logout(@RequestHeader("token") String token){
        log.info("管理员{}退出登录", CurrentHolder.getCurrent());
        // Token加入黑名单，TTL为剩余有效期
        long remaining = JwtUtils.getRemainingMillis(token);
        if (remaining > 0) {
            redisTemplate.opsForValue().set("token:blacklist:" + token, "1", remaining, TimeUnit.MILLISECONDS);
        }
        CurrentHolder.removeCurrent();
        return Result.success();
    }

    @GetMapping("/getByUsername/{username}")
    public Result getByUsername(@PathVariable String username) {
        log.info("管理员根据username:{}查询",username);
        Manager manager = managerService.getByUsername(username);
        return Result.success(manager);
    }

    @GetMapping("/page")
    public Result getManagerByPage(ManagerPageQueryDTO managerPageQueryDTO) {
        log.info("管理员分页查询");
        return Result.success(managerService.getManagerByPage(managerPageQueryDTO));
    }
}

package com.yilena.service.controller.admin;

import com.yilena.service.entity.Result;
import com.yilena.service.entity.dto.UserPendingPageQueryDTO;
import com.yilena.service.entity.dto.UserPendingStatusDTO;
import com.yilena.service.log.LogOperation;
import com.yilena.service.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员用户审核控制器
 * 
 * 功能说明：
 * 1. 提供管理员用户状态管理
 * 2. 支持更新用户状态
 * 3. 支持分页查询用户列表
 * 
 * 请求路径：/manager/user
 * 
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/manager/user")
public class UserPendingController {

    private final UserService userService;

    @LogOperation
    @PutMapping("/status")
    public Result updateUserStatus(@RequestBody UserPendingStatusDTO userPendingStatusDTO) {
        log.info("更新用户状态：{}", userPendingStatusDTO);
        userService.updateUserStatus(userPendingStatusDTO);
        return Result.success();
    }

    @GetMapping("/page")
    public Result getUserByPage(UserPendingPageQueryDTO userPendingPageQueryDTO){
        log.info("用户分页查询");
        return Result.success(userService.getUserPendingByPage(userPendingPageQueryDTO));
    }
}

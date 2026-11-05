package com.yilena.service.controller.admin;

import com.yilena.service.entity.Result;
import com.yilena.service.entity.dto.PostPendingDTO;
import com.yilena.service.entity.dto.PostPendingPageQueryDTO;
import com.yilena.service.log.LogOperation;
import com.yilena.service.service.PostPendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员动态审核控制器
 * 
 * 功能说明：
 * 1. 提供管理员动态审核功能
 * 2. 支持更新动态审核状态
 * 3. 支持分页查询待审核动态列表
 * 
 * 请求路径：/manager/post
 * 
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/manager/post")
public class PostPendingController {
    private final PostPendingService postPendingService;

    @LogOperation
    @PutMapping
    public Result updatePostPendingStatus(@RequestBody PostPendingDTO postPendingDTO){
        log.info("下架动态：{}", postPendingDTO);
        postPendingService.updatePostPendingStatus(postPendingDTO);
        return Result.success();
    }

    @GetMapping("/page")
    public Result getPostPendingByPage(PostPendingPageQueryDTO postPendingPageQueryDTO){
        log.info("分页查询动态：{}", postPendingPageQueryDTO);
        return Result.success(postPendingService.getPostPendingByPage(postPendingPageQueryDTO));
    }
}

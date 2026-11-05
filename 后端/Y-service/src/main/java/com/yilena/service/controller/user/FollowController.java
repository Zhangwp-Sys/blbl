package com.yilena.service.controller.user;

import com.yilena.service.entity.Result;
import com.yilena.service.entity.dto.FollowDTO;
import com.yilena.service.entity.dto.FollowPageDTO;
import com.yilena.service.service.FollowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

/**
 * 关注控制器
 * 
 * 功能说明：
 * 1. 处理用户关注相关的HTTP请求
 * 2. 提供关注和取消关注功能
 * 3. 支持查询关注列表和关注状态
 * 
 * API接口：
 * - POST /follow - 关注用户
 * - DELETE /follow/{followedId}/{followerId}/{isSpecial} - 取消关注
 * - GET /follow/search/page - 分页查询关注列表
 * - GET /follow/{userId}/{authorId} - 查询关注状态
 * 
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/follow")
public class FollowController {

    private final FollowService followService;

    /**
     * 关注用户
     * 
     * 功能：关注指定用户
     * 业务逻辑：
     * 1. 接收关注信息
     * 2. 清除关注缓存
     * 3. 保存关注记录
     * 4. 更新关注计数
     * 
     * @param followDTO 关注信息DTO
     * @return Result 操作结果
     */
    @CacheEvict(value = "follow", key = "#followDTO.getFollowerId")
    @PostMapping
    public Result follow(@RequestBody FollowDTO followDTO){
        log.info("用户{}关注了用户{}", followDTO.getFollowerId(), followDTO.getFollowedId());
        followService.follow(followDTO);
        return Result.success();
    }

    /**
     * 取消关注
     * 
     * 功能：取消对指定用户的关注
     * 业务逻辑：
     * 1. 接收取消关注信息
     * 2. 清除关注缓存
     * 3. 删除关注记录
     * 4. 更新关注计数
     * 
     * @param followedId 被关注用户ID
     * @param followerId 关注者用户ID
     * @param isSpecial 是否特别关注（0:普通关注，1:特别关注）
     * @return Result 操作结果
     */
    @CacheEvict(value = "follow", key = "#followerId")
    @DeleteMapping("/{followedId}/{followerId}/{isSpecial}")
    public Result unfollow(@PathVariable Long followedId, @PathVariable Long followerId, @PathVariable Integer isSpecial){
        log.info("用户{}取消关注了用户{}", followerId, followedId);
        FollowDTO followDTO = FollowDTO.builder().followedId(followedId).followerId(followerId).isSpecial(isSpecial).build();
        followService.unfollow(followDTO);
        return Result.success();
    }

    /**
     * 分页查询关注列表
     * 
     * 功能：分页获取用户的关注列表
     * 业务逻辑：
     * 1. 根据用户ID查询关注列表
     * 2. 支持分页功能
     * 3. 使用缓存提高查询性能
     * 
     * @param followPageDTO 关注分页查询DTO
     * @return Result 包含分页关注列表
     */
    @Cacheable(value = "follow", key = "#followPageDTO.id")
    @GetMapping("/search/page")
    public Result getFollowByPage(FollowPageDTO followPageDTO){
        log.info("用户{}查询了关注列表", followPageDTO.getId());
        return Result.success(followService.getFollowByPage(followPageDTO));
    }

    /**
     * 查询关注状态
     * 
     * 功能：查询用户是否关注了指定用户
     * 业务逻辑：
     * 1. 接收用户ID和作者ID
     * 2. 查询关注记录
     * 3. 返回关注状态
     * 
     * @param userId 用户ID
     * @param authorId 作者ID
     * @return Result 包含关注状态
     */
    @GetMapping("/{userId}/{authorId}")
    public Result isFollow(@PathVariable Long userId, @PathVariable Long authorId){
        log.info("用户{}查询了用户{}的关注状态", userId, authorId);
        return Result.success(followService.isFollow(userId, authorId));
    }
}

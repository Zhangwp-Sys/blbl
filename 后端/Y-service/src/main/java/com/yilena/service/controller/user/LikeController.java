package com.yilena.service.controller.user;

import com.yilena.service.entity.Result;
import com.yilena.service.entity.po.Like;
import com.yilena.service.service.LikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

/**
 * 点赞控制器
 * 
 * 功能说明：
 * 1. 处理点赞相关的HTTP请求
 * 2. 提供点赞和取消点赞功能
 * 3. 支持查询点赞状态
 * 
 * API接口：
 * - PUT /like/do - 点赞
 * - PUT /like/undo - 取消点赞
 * - GET /like/search/{userId}/{entityId}/{entityType} - 查询点赞状态
 * 
 * 点赞实体类型：
 * - 0: 视频
 * - 1: 评论
 * - 2: 帖子
 * 
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/like")
public class LikeController {

    private final LikeService likeService;

    /**
     * 点赞
     * 
     * 功能：为视频、评论或帖子点赞
     * 业务逻辑：
     * 1. 接收点赞信息
     * 2. 清除相关缓存
     * 3. 保存点赞记录
     * 4. 更新点赞计数
     * 
     * @param like 点赞实体对象
     * @return Result 操作结果
     */
    @CacheEvict(value = "postWhichFollowing", key = "#like.userId")
    @PutMapping("/do")
    public Result doLike(@RequestBody Like like){
        log.info("点赞");
        likeService.doLike(like);
        return Result.success();
    }

    /**
     * 取消点赞
     * 
     * 功能：取消对视频、评论或帖子的点赞
     * 业务逻辑：
     * 1. 接收点赞信息
     * 2. 清除相关缓存
     * 3. 删除点赞记录
     * 4. 更新点赞计数
     * 
     * @param like 点赞实体对象
     * @return Result 操作结果
     */
    @CacheEvict(value = "postWhichFollowing", key = "#like.userId")
    @PutMapping("/undo")
    public Result undoLike(@RequestBody Like like){
        log.info("取消点赞");
        likeService.undoLike(like);
        return Result.success();
    }

    /**
     * 查询点赞状态
     * 
     * 功能：查询用户是否已点赞指定实体
     * 业务逻辑：
     * 1. 接收用户ID、实体ID和实体类型
     * 2. 查询点赞记录
     * 3. 返回点赞状态
     * 
     * @param userId 用户ID
     * @param entityId 实体ID
     * @param entityType 实体类型（0:视频，1:评论，2:帖子）
     * @return Result 包含点赞状态
     */
    @GetMapping("/search/{userId}/{entityId}/{entityType}")
    public Result isLike(@PathVariable("userId") Long userId,
                         @PathVariable("entityId") Long entityId,
                         @PathVariable("entityType") Integer entityType){
        log.info("查询点赞状态");
        Like like = new Like(null, entityId, entityType, userId, null);
        return Result.success(likeService.isLike(like));
    }
}

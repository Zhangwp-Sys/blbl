package com.yilena.service.controller.user;

import com.yilena.service.entity.Result;
import com.yilena.service.entity.dto.VideoDTO;
import com.yilena.service.entity.po.Video;
import com.yilena.service.log.LogOperation;
import com.yilena.service.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * 视频控制器
 * 
 * 功能说明：
 * 1. 处理视频相关的HTTP请求
 * 2. 提供视频的增删改查功能
 * 3. 支持视频分页查询和推荐
 * 4. 提供用户视频统计数据查询
 * 
 * API接口：
 * - POST /video - 添加视频
 * - GET /video/search/page - 分页查询视频
 * - GET /video/{id} - 根据ID查询视频
 * - PUT /video - 更新视频信息
 * - DELETE /video/{id} - 删除视频
 * - GET /video/search/list - 获取所有视频
 * - GET /video/search/leftRecommend - 获取左侧推荐视频
 * - GET /video/allViews/{userId} - 获取用户视频总播放量
 * - GET /video/allCoins/{userId} - 获取用户视频总硬币数
 * - GET /video/allComments/{userId} - 获取用户视频总评论数
 * - GET /video/allBarrages/{userId} - 获取用户视频总弹幕数
 * - GET /video/allLikes/{userId} - 获取用户视频总点赞数
 * - GET /video/allShares/{userId} - 获取用户视频总分享数
 * - GET /video/allFavorites/{userId} - 获取用户视频总收藏数
 * - GET /video/search/user/{userId} - 获取用户视频列表
 * - GET /video/search/userInfo/{userId}/{sortType} - 获取用户主页视频
 * 
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/video")
public class VideoController {

    private final VideoService videoService;

    /**
     * 添加视频
     * 
     * 功能：将视频添加到待审核列表
     * 业务逻辑：
     * 1. 接收视频信息
     * 2. 清除相关缓存
     * 3. 记录操作日志
     * 4. 将视频添加到待审核队列
     * 
     * @param video 视频实体对象
     * @return Result 操作结果
     */
    @CacheEvict(value = "videoPendings", key = "#video.userId + ':' + 0")
    @LogOperation
    @PostMapping
    public Result addVideo(@RequestBody Video video){
        log.info("添加视频{}", video);
        videoService.addVideoToPending(video);
        return Result.success();
    }

    /**
     * 分页查询视频
     * 
     * 功能：根据查询条件分页获取视频列表
     * 业务逻辑：
     * 1. 支持按关键词、分类、用户ID等条件查询
     * 2. 支持分页功能
     * 3. 支持排序功能
     * 
     * @param videoDTO 视频查询条件DTO
     * @return Result 包含分页视频列表
     */
    @Transactional
    @GetMapping("/search/page")
    public Result getVideoByPage(VideoDTO videoDTO){
        log.info("用户查询视频分页信息");
        return Result.success(videoService.getVideoByPage(videoDTO));
    }

    /**
     * 根据ID查询视频
     * 
     * 功能：根据视频ID获取视频详细信息
     * 业务逻辑：
     * 1. 根据ID查询视频
     * 2. 包含视频基本信息、作者信息等
     * 
     * @param id 视频ID
     * @return Result 包含视频详细信息
     */
    @GetMapping("/{id}")
    public Result getVideoById(@PathVariable Long id){
        log.info("用户查询视频信息");
        return Result.success(videoService.getVideoById(id));
    }

    /**
     * 更新视频信息
     * 
     * 功能：更新视频的基本信息
     * 业务逻辑：
     * 1. 接收更新后的视频信息
     * 2. 记录操作日志
     * 3. 更新数据库中的视频记录
     * 
     * @param video 视频实体对象
     * @return Result 操作结果
     */
    @LogOperation
    @PutMapping
    public Result updateVideo(@RequestBody Video video){
        log.info("更新视频{}", video);
        videoService.updateVideo(video);
        return Result.success();
    }

    /**
     * 删除视频
     * 
     * 功能：根据ID删除视频
     * 业务逻辑：
     * 1. 记录操作日志
     * 2. 删除视频记录
     * 3. 清除相关缓存
     * 
     * @param id 视频ID
     * @return Result 操作结果
     */
    @LogOperation
    @DeleteMapping("/{id}")
    public Result deleteVideo(@PathVariable Long id){
        log.info("删除视频{}", id);
        videoService.deleteVideo(id);
        return Result.success();
    }

    /**
     * 获取所有视频
     * 
     * 功能：获取系统中所有视频列表
     * 业务逻辑：
     * 1. 查询所有视频记录
     * 2. 返回视频列表
     * 
     * @return Result 包含所有视频列表
     */
    @GetMapping("/search/list")
    public Result getAllVideos(){
        log.info("获取所有视频");
        return Result.success(videoService.getAllVideos());
    }

    /**
     * 获取左侧推荐视频
     * 
     * 功能：获取首页左侧推荐视频列表
     * 业务逻辑：
     * 1. 根据推荐算法获取视频
     * 2. 返回推荐视频列表
     * 
     * @return Result 包含推荐视频列表
     */
    @GetMapping("/search/leftRecommend")
    public Result getLeftRecommendVideos(){
        log.info("获取左侧推荐视频");
        return Result.success(videoService.getLeftRecommendVideos());
    }

    /**
     * 获取用户视频总播放量
     * 
     * 功能：统计用户所有视频的总播放量
     * 业务逻辑：
     * 1. 根据用户ID查询所有视频
     * 2. 统计视频播放量总和
     * 3. 使用Redis缓存提高查询性能
     * 
     * @param userId 用户ID
     * @return Result 包含总播放量
     */
    @Cacheable(value = "allViews",key = "#userId", sync = true)
    @GetMapping("/allViews/{userId}")
    public Result getUserVideoAllViews(@PathVariable Long userId){
        log.info("获取当前用户视频的所有播放量");
        return Result.success(videoService.getUserVideoAllViews(userId));
    }

    /**
     * 获取用户视频总硬币数
     * 
     * 功能：统计用户所有视频获得的硬币总数
     * 业务逻辑：
     * 1. 根据用户ID查询所有视频
     * 2. 统计视频硬币数总和
     * 3. 使用Redis缓存提高查询性能
     * 
     * @param userId 用户ID
     * @return Result 包含总硬币数
     */
    @Cacheable(value = "allCoins",key = "#userId", sync = true)
    @GetMapping("/allCoins/{userId}")
    public Result getUserVideoAllCoins(@PathVariable Long userId){
        log.info("获取当前用户视频的所有硬币");
        return Result.success(videoService.getUserVideoAllCoins(userId));
    }

    /**
     * 获取用户视频总评论数
     * 
     * 功能：统计用户所有视频的评论总数
     * 业务逻辑：
     * 1. 根据用户ID查询所有视频
     * 2. 统计视频评论数总和
     * 3. 使用Redis缓存提高查询性能
     * 
     * @param userId 用户ID
     * @return Result 包含总评论数
     */
    @Cacheable(value = "allComments",key = "#userId", sync = true)
    @GetMapping("/allComments/{userId}")
    public Result getUserVideoAllComments(@PathVariable Long userId){
        log.info("获取当前用户视频的所有评论");
        return Result.success(videoService.getUserVideoAllComments(userId));
    }

    /**
     * 获取用户视频总弹幕数
     * 
     * 功能：统计用户所有视频的弹幕总数
     * 业务逻辑：
     * 1. 根据用户ID查询所有视频
     * 2. 统计视频弹幕数总和
     * 3. 使用Redis缓存提高查询性能
     * 
     * @param userId 用户ID
     * @return Result 包含总弹幕数
     */
    @Cacheable(value = "allBarrages",key = "#userId", sync = true)
    @GetMapping("/allBarrages/{userId}")
    public Result getUserVideoAllBarrages(@PathVariable Long userId){
        log.info("获取当前用户视频的所有弹幕");
        return Result.success(videoService.getUserVideoAllBarrages(userId));
    }

    /**
     * 获取用户视频总点赞数
     * 
     * 功能：统计用户所有视频的点赞总数
     * 业务逻辑：
     * 1. 根据用户ID查询所有视频
     * 2. 统计视频点赞数总和
     * 3. 使用Redis缓存提高查询性能
     * 
     * @param userId 用户ID
     * @return Result 包含总点赞数
     */
    @Cacheable(value = "allLikes",key = "#userId", sync = true)
    @GetMapping("/allLikes/{userId}")
    public Result getUserVideoAllLikes(@PathVariable Long userId){
        log.info("获取当前用户视频的所有点赞");
        return Result.success(videoService.getUserVideoAllLikes(userId));
    }

    /**
     * 获取用户视频总分享数
     * 
     * 功能：统计用户所有视频的分享总数
     * 业务逻辑：
     * 1. 根据用户ID查询所有视频
     * 2. 统计视频分享数总和
     * 3. 使用Redis缓存提高查询性能
     * 
     * @param userId 用户ID
     * @return Result 包含总分享数
     */
    @Cacheable(value = "allShares",key = "#userId", sync = true)
    @GetMapping("/allShares/{userId}")
    public Result getUserVideoAllShares(@PathVariable Long userId){
        log.info("获取当前用户视频的所有分享");
        return Result.success(videoService.getUserVideoAllShares(userId));
    }

    /**
     * 获取用户视频总收藏数
     * 
     * 功能：统计用户所有视频的收藏总数
     * 业务逻辑：
     * 1. 根据用户ID查询所有视频
     * 2. 统计视频收藏数总和
     * 3. 使用Redis缓存提高查询性能
     * 
     * @param userId 用户ID
     * @return Result 包含总收藏数
     */
    @Cacheable(value = "allFavorites",key = "#userId", sync = true)
    @GetMapping("/allFavorites/{userId}")
    public Result getUserVideoAllFavorites(@PathVariable Long userId){
        log.info("获取当前用户视频的所有收藏");
        return Result.success(videoService.getUserVideoAllFavorites(userId));
    }

    /**
     * 获取用户视频列表
     * 
     * 功能：根据用户ID获取该用户的所有视频
     * 业务逻辑：
     * 1. 根据用户ID查询视频
     * 2. 返回用户视频列表
     * 
     * @param userId 用户ID
     * @return Result 包含用户视频列表
     */
    @GetMapping("/search/user/{userId}")
    public Result getVideoByUserId(@PathVariable Long userId){
        log.info("获取当前用户视频");
        return Result.success(videoService.getVideoByUserId(userId));
    }

    /**
     * 获取用户主页视频
     * 
     * 功能：获取用户主页展示的视频列表
     * 业务逻辑：
     * 1. 根据用户ID和排序类型查询视频
     * 2. 支持按时间、热度等排序
     * 3. 使用Redis缓存提高查询性能
     * 
     * @param userId 用户ID
     * @param sortType 排序类型（0:时间，1:热度等）
     * @return Result 包含用户主页视频列表
     */
    @Cacheable(value = "userInfoVideo",key = "#userId+ ':' + #sortType")
    @GetMapping("/search/userInfo/{userId}/{sortType}")
    public Result getUserInfoVideos(@PathVariable Long userId, @PathVariable Integer sortType){
        log.info("获取当前用户主页视频");
        return Result.success(videoService.getUserInfoVideos(userId,sortType));
    }
}

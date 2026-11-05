package com.yilena.service.controller.user;

import com.yilena.service.entity.Result;
import com.yilena.service.service.UserReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户数据统计控制器
 * 
 * 功能说明：
 * 1. 提供用户粉丝统计报表
 * 2. 提供点赞统计报表
 * 3. 提供收藏统计报表
 * 4. 提供投币统计报表
 * 5. 提供评论统计报表
 * 
 * 请求路径：/userReport
 * 
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/userReport")
public class UserReportController {

    private final UserReportService userReportService;

    @Cacheable(value = "userReportFollowers",key="#userId + ':' + #type")
    @GetMapping("/followers/{userId}/{type}")
    public Result getUserReportFollowers(@PathVariable Long userId, @PathVariable Integer type){
        log.info("用户{}查询粉丝统计", userId);
        return Result.success(userReportService.getUserReportFollowers(userId, type));
    }

    @Cacheable(value = "userReportLikes",key="#userId + ':' + #type")
    @GetMapping("/likes/{userId}/{type}")
    public Result getUserReportLikes(@PathVariable Long userId, @PathVariable Integer type){
        log.info("用户{}查询点赞统计", userId);
        return Result.success(userReportService.getUserReportLikes(userId, type));
    }

    @Cacheable(value = "userReportFavorites",key="#userId + ':' + #type")
    @GetMapping("/favorites/{userId}/{type}")
    public Result getUserReportFavorites(@PathVariable Long userId, @PathVariable Integer type){
        log.info("用户{}查询收藏统计", userId);
        return Result.success(userReportService.getUserReportFavorites(userId, type));
    }

    @Cacheable(value = "userReportCoins",key="#userId + ':' + #type")
    @GetMapping("/coins/{userId}/{type}")
    public Result getUserReportCoins(@PathVariable Long userId, @PathVariable Integer type){
        log.info("用户{}查询投币统计", userId);
        return Result.success(userReportService.getUserReportCoins(userId, type));
    }

    @Cacheable(value = "userReportComment",key="#userId + ':' + #type")
    @GetMapping("/comments/{userId}/{type}")
    public Result getUserReportComments(@PathVariable Long userId, @PathVariable Integer type){
        log.info("用户{}查询评论统计", userId);
        return Result.success(userReportService.getUserReportComments(userId, type));
    }
}

package com.yilena.service.controller.admin;

import com.yilena.service.constant.StatusConstant;
import com.yilena.service.entity.Result;
import com.yilena.service.entity.dto.VideoPendingDTO;
import com.yilena.service.entity.dto.VideoPendingStatusDTO;
import com.yilena.service.log.LogOperation;
import com.yilena.service.service.VideoPendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员视频审核控制器
 * 
 * 功能说明：
 * 1. 提供管理员视频审核分页查询
 * 2. 支持更新视频审核状态
 * 3. 支持视频下架操作
 * 
 * 请求路径：/manager/pending
 * 
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/manager/pending")
public class VideoPendingController {

    private final VideoPendingService videoPendingService;

    @Cacheable(value = "videoPage",key = "#videoPendingDTO.status + ':' + #videoPendingDTO.pageSize + ':' + #videoPendingDTO.page")
    @GetMapping("/page")
    public Result getVideoPendingByPageWhichWait(VideoPendingDTO videoPendingDTO){
        log.info("分页查询视频");
        return Result.success(videoPendingService.getVideoPendingByPage(videoPendingDTO));
    }

    @GetMapping("/{id}")
    public Result getVideoPendingById(@PathVariable Long id){
        log.info("查询待审核视频");
        return Result.success(videoPendingService.getVideoPendingById(id));
    }

    @LogOperation
    @Caching(
            evict = {
                    @CacheEvict(value = "videoPage", allEntries = true),
                    @CacheEvict(value = "videoPendings", allEntries = true)
            }
    )
    @PutMapping("/status")
    public Result updateVideoPendingStatus(@RequestBody VideoPendingStatusDTO videoPendingStatusDTO){
        log.info("更新待审核视频状态");
        videoPendingService.updateVideoPendingStatus(videoPendingStatusDTO);
        return Result.success();
    }

    @LogOperation
    @CacheEvict(value = "videoPage",allEntries = true)
    @PutMapping("/video")
    public Result updateVideoPendingFromVideo(@RequestBody VideoPendingStatusDTO videoPendingStatusDTO){
        log.info("下架视频");
        videoPendingService.updateVideoPendingFromVideo(videoPendingStatusDTO);
        return Result.success();
    }
}

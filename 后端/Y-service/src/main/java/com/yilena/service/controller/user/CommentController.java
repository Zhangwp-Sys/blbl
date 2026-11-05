package com.yilena.service.controller.user;

import com.yilena.service.entity.Result;
import com.yilena.service.entity.dto.CommentQueryDTO;
import com.yilena.service.entity.po.Comment;
import com.yilena.service.log.LogOperation;
import com.yilena.service.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器
 * 
 * 功能说明：
 * 1. 处理评论相关的HTTP请求
 * 2. 提供评论的增删改查功能
 * 3. 支持评论的排序和筛选
 * 
 * API接口：
 * - POST /comment - 添加评论
 * - GET /comment/search/hotOrNew - 按热度或时间查询评论
 * - DELETE /comment - 删除评论
 * - GET /comment/query - 条件查询评论
 * 
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/comment")
public class CommentController {

    private final CommentService commentService;

    /**
     * 添加评论
     * 
     * 功能：为视频或帖子添加评论
     * 业务逻辑：
     * 1. 接收评论内容
     * 2. 记录操作日志
     * 3. 保存评论到数据库
     * 4. 更新评论计数
     * 
     * @param comment 评论实体对象
     * @return Result 操作结果
     */
    @LogOperation
    @PostMapping
    public Result addComment(@RequestBody Comment comment){
        log.info("添加评论：{}",comment);
        commentService.addComment(comment);
        return Result.success();
    }

    /**
     * 按热度或时间查询评论
     * 
     * 功能：根据排序方式获取评论列表
     * 业务逻辑：
     * 1. 支持按热度（点赞数）排序
     * 2. 支持按时间排序
     * 3. 返回评论列表
     * 
     * @param comment 评论查询条件
     * @return Result 包含评论列表
     */
    @GetMapping("/search/hotOrNew")
    public Result getCommentByList(Comment comment){
        log.info("查询评论：{}",comment);
        return Result.success(commentService.getCommentByList(comment));
    }

//    @PutMapping
//    public Result updateComment(@RequestBody Comment comment){
//        log.info("修改评论：{}",comment);
//        commentService.updateComment(comment);
//        return Result.success();
//    }

    /**
     * 删除评论
     * 
     * 功能：批量删除评论
     * 业务逻辑：
     * 1. 接收评论ID列表
     * 2. 记录操作日志
     * 3. 删除评论记录
     * 4. 更新评论计数
     * 
     * @param ids 评论ID列表
     * @return Result 操作结果
     */
    @LogOperation
    @DeleteMapping
    public Result deleteComment(@RequestParam List<Long> ids){
        log.info("删除评论：{}", ids);
        commentService.deleteComment(ids);
        return Result.success();
    }

    /**
     * 条件查询评论
     * 
     * 功能：根据复杂条件查询评论
     * 业务逻辑：
     * 1. 支持多条件组合查询
     * 2. 支持分页功能
     * 3. 支持排序功能
     * 
     * @param commentQueryDTO 评论查询条件DTO
     * @return Result 包含评论列表
     */
    @GetMapping("/query")
    public Result getCommentQuery(CommentQueryDTO commentQueryDTO){
        log.info("条件查询评论：{}",commentQueryDTO);
        return Result.success(commentService.getCommentQuery(commentQueryDTO));
    }
}

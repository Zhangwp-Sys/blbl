package com.yilena.service.dao;

import com.yilena.service.entity.po.Comment;
import com.yilena.service.entity.vo.CommentVO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论数据访问接口
 * 
 * 功能说明：
 * 1. 定义评论相关的数据库操作方法
 * 2. 提供评论CRUD操作
 * 3. 支持评论查询和统计
 * 
 * 方法说明：
 * - addComment: 添加评论
 * - getCommentById: 根据ID查询评论
 * - updateComment: 更新评论
 * - getCommentByHot: 按热度查询评论
 * - getCommentByNew: 按时间查询评论
 * - deleteComment: 删除评论
 * - getCommentByLastId: 根据父评论ID查询子评论
 * - getCommentByLastIdAndUserId: 根据父评论ID和用户ID查询评论
 * - getCommentsQueryByNewSortType/LikeSortType/MostCommentsSortType: 按不同排序方式查询评论
 * - getCommentByIds: 批量查询评论
 * - deleteCommentByIds: 批量删除评论
 * - getTotalComment: 获取评论总数
 * 
 */
@Mapper
public interface CommentMapper {

    /**
     * 添加评论
     * 
     * @param comment 评论实体对象
     */
    void addComment(Comment comment);

    /**
     * 根据ID查询评论
     * 
     * @param entityId 评论ID
     * @return 评论实体对象
     */
    Comment getCommentById(Long entityId);

    /**
     * 更新评论
     * 
     * @param comment1 评论实体对象
     */
    void updateComment(Comment comment1);

    /**
     * 按热度查询评论
     * 
     * @param comment 评论查询条件
     * @return 评论VO列表
     */
    List<CommentVO> getCommentByHot(Comment comment);

    /**
     * 按时间查询评论
     * 
     * @param comment 评论查询条件
     * @return 评论VO列表
     */
    List<CommentVO> getCommentByNew(Comment comment);

    /**
     * 删除评论
     * 
     * @param id 评论ID
     */
    void deleteComment(Long id);

    /**
     * 根据父评论ID查询子评论
     * 
     * @param lastId 父评论ID
     * @return 子评论列表
     */
    List<Comment> getCommentByLastId(Long lastId);

    /**
     * 根据父评论ID和用户ID查询评论
     * 
     * @param lastId 父评论ID
     * @param userId 用户ID
     * @return 评论实体对象
     */
    Comment getCommentByLastIdAndUserId(Long lastId, Long userId);

    /**
     * 按时间排序查询评论
     * 
     * @param videoIds 视频ID列表
     * @return 评论列表
     */
    List<Comment> getCommentsQueryByNewSortType(List<Long> videoIds);

    /**
     * 按点赞数排序查询评论
     * 
     * @param videoIds 视频ID列表
     * @return 评论列表
     */
    List<Comment> getCommentsQueryByLikeSortType(List<Long> videoIds);

    /**
     * 按评论数排序查询评论
     * 
     * @param videoIds 视频ID列表
     * @return 评论列表
     */
    List<Comment> getCommentsQueryByMostCommentsSortType(List<Long> videoIds);

    /**
     * 批量查询评论
     * 
     * @param ids 评论ID列表
     * @return 评论列表
     */
    List<Comment> getCommentByIds(List<Long> ids);

    /**
     * 批量删除评论
     * 
     * @param ids 评论ID列表
     */
    void deleteCommentByIds(List<Long> ids);

    /**
     * 获取评论总数
     * 
     * @return 评论总数
     */
    Long getTotalComment();
}

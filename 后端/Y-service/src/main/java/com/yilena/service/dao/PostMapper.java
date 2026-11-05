package com.yilena.service.dao;

import com.yilena.service.entity.dto.PostPendingPageQueryDTO;
import com.yilena.service.entity.po.Post;
import com.yilena.service.entity.vo.PostVO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子数据访问接口
 * 
 * 功能说明：
 * 1. 定义帖子相关的数据库操作方法
 * 2. 提供帖子CRUD操作
 * 3. 支持帖子查询和统计
 * 
 * 方法说明：
 * - addPost: 添加帖子
 * - getPostById: 根据ID查询帖子
 * - updatePost: 更新帖子
 * - deletePost: 删除帖子
 * - getPostByUserIds: 批量查询用户帖子
 * - getMyPost: 查询我的帖子
 * - getPostByUserId: 根据用户ID查询帖子
 * - getTotalPost: 获取帖子总数
 * - getPostPendingByPage: 分页查询待审核帖子
 * - getPostUploadReport: 获取帖子上传统计
 * 
 */
@Mapper
public interface PostMapper {

    /**
     * 添加帖子
     * 
     * @param post 帖子实体对象
     */
    void addPost(Post post);

    /**
     * 根据ID查询帖子
     * 
     * @param id 帖子ID
     * @return 帖子实体对象
     */
    Post getPostById(Long id);

    /**
     * 更新帖子
     * 
     * @param post 帖子实体对象
     */
    void updatePost(Post post);

    /**
     * 删除帖子
     * 
     * @param id 帖子ID
     */
    void deletePost(Long id);

    /**
     * 批量查询用户帖子
     * 
     * @param followingIds 用户ID列表
     * @return 帖子VO列表
     */
    List<PostVO> getPostByUserIds(List<Long> followingIds);

    /**
     * 查询我的帖子
     * 
     * @param userId 用户ID
     * @return 帖子VO列表
     */
    List<PostVO> getMyPost(Long userId);

    /**
     * 根据用户ID查询帖子
     * 
     * @param userId 用户ID
     * @return 帖子VO列表
     */
    List<PostVO> getPostByUserId(Long userId);

    /**
     * 获取帖子总数
     * 
     * @return 帖子总数
     */
    Long getTotalPost();

    /**
     * 分页查询待审核帖子
     * 
     * @param postPendingPageQueryDTO 分页查询条件
     * @return 待审核帖子列表
     */
    List<PostVO> getPostPendingByPage(PostPendingPageQueryDTO postPendingPageQueryDTO);

    /**
     * 获取帖子上传统计
     * 
     * @param beginDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 时间段内上传帖子数
     */
    Integer getPostUploadReport(LocalDateTime beginDateTime, LocalDateTime endDateTime);
}

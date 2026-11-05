package com.yilena.service.dao;

import com.yilena.service.entity.PageResult;
import com.yilena.service.entity.dto.VideoPendingDTO;
import com.yilena.service.entity.po.Video;
import com.yilena.service.entity.po.VideoPending;
import com.yilena.service.entity.vo.VideoVO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频数据访问接口
 * 
 * 功能说明：
 * 1. 定义视频相关的数据库操作方法
 * 2. 提供视频CRUD操作
 * 3. 支持视频查询、统计和推荐
 * 
 * 方法说明：
 * - getVideoById: 根据ID查询视频
 * - updateVideo: 更新视频信息
 * - deleteVideo: 删除视频
 * - getVideoCoins: 批量查询视频硬币数
 * - addVideo: 添加视频
 * - getLeftRecommendVideos: 获取左侧推荐视频
 * - getUserVideoAllViews/Coins/Comments/Barrages/Likes/Shares/Favorites: 获取用户视频统计数据
 * - getVideoByUserId: 根据用户ID查询视频
 * - getVideoIdByUserId: 根据用户ID和标题查询视频ID
 * - getUserInfoVideosByNew/Views/Favorites: 获取用户主页视频（按不同排序方式）
 * - getVideoByIds: 批量查询视频
 * - getFollowingFromReportCount/LikeFromReportCount/FavoriteFromReportCount/CoinFromReportCount/CommentFromReportCount: 获取用户视频统计数据
 * - getVideoCount: 获取视频总数
 * - getVideoByPage: 分页查询待审核视频
 * - getAllVideoByUserId: 获取用户所有视频
 * - getVideoUploadReport: 获取视频上传统计
 * 
 */
@Mapper
public interface VideoMapper {

    /**
     * 根据ID查询视频
     * 
     * @param id 视频ID
     * @return 视频实体对象
     */
    Video getVideoById(Long id);

    /**
     * 更新视频信息
     * 
     * @param video 视频实体对象
     */
    void updateVideo(Video video);

    /**
     * 删除视频
     * 
     * @param id 视频ID
     */
    void deleteVideo(Long id);

    /**
     * 批量查询视频硬币数
     * 
     * @param videoIds 视频ID列表
     * @return 视频VO列表（包含硬币数）
     */
    List<VideoVO> getVideoCoins(List<Long> videoIds);

    /**
     * 添加视频
     * 
     * @param video 视频实体对象
     */
    void addVideo(Video video);

    /**
     * 获取左侧推荐视频
     * 
     * @return 推荐视频列表
     */
    List<VideoVO> getLeftRecommendVideos();

    /**
     * 获取用户视频总播放量
     * 
     * @param userId 用户ID
     * @return 总播放量
     */
    Long getUserVideoAllViews(Long userId);

    /**
     * 获取用户视频总硬币数
     * 
     * @param userId 用户ID
     * @return 总硬币数
     */
    Long getUserVideoAllCoins(Long userId);

    /**
     * 获取用户视频总评论数
     * 
     * @param userId 用户ID
     * @return 总评论数
     */
    Long getUserVideoAllComments(Long userId);

    /**
     * 获取用户视频总弹幕数
     * 
     * @param userId 用户ID
     * @return 总弹幕数
     */
    Long getUserVideoAllBarrages(Long userId);

    /**
     * 获取用户视频总点赞数
     * 
     * @param userId 用户ID
     * @return 总点赞数
     */
    Long getUserVideoAllLikes(Long userId);

    /**
     * 获取用户视频总分享数
     * 
     * @param userId 用户ID
     * @return 总分享数
     */
    Long getUserVideoAllShares(Long userId);

    /**
     * 获取用户视频总收藏数
     * 
     * @param userId 用户ID
     * @return 总收藏数
     */
    Long getUserVideoAllFavorites(Long userId);

    /**
     * 根据用户ID查询视频
     * 
     * @param userId 用户ID
     * @return 视频列表
     */
    List<Video> getVideoByUserId(Long userId);

    /**
     * 根据用户ID和标题查询视频ID
     * 
     * @param currentUserId 用户ID
     * @param videoTitle 视频标题
     * @return 视频ID列表
     */
    List<Long> getVideoIdByUserId(Long currentUserId, String videoTitle);

    /**
     * 获取用户主页视频（按时间排序）
     * 
     * @param userId 用户ID
     * @return 视频VO列表
     */
    List<VideoVO> getUserInfoVideosByNew(Long userId);

    /**
     * 获取用户主页视频（按播放量排序）
     * 
     * @param userId 用户ID
     * @return 视频VO列表
     */
    List<VideoVO> getUserInfoVideosByViews(Long userId);

    /**
     * 获取用户主页视频（按收藏数排序）
     * 
     * @param userId 用户ID
     * @return 视频VO列表
     */
    List<VideoVO> getUserInfoVideosByFavorites(Long userId);

    /**
     * 批量查询视频
     * 
     * @param videoIds 视频ID列表
     * @return 视频VO列表
     */
    List<VideoVO> getVideoByIds(List<Long> videoIds);

    /**
     * 获取用户关注数统计
     * 
     * @param userId 用户ID
     * @param beginDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 关注数
     */
    Integer getFollowingFromReportCount(Long userId, LocalDateTime beginDateTime, LocalDateTime endDateTime);

    /**
     * 获取用户点赞数统计
     * 
     * @param userId 用户ID
     * @param beginDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 点赞数
     */
    Integer getLikeFromReportCount(Long userId, LocalDateTime beginDateTime, LocalDateTime endDateTime);

    /**
     * 获取用户收藏数统计
     * 
     * @param userId 用户ID
     * @param beginDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 收藏数
     */
    Integer getFavoriteFromReportCount(Long userId, LocalDateTime beginDateTime, LocalDateTime endDateTime);

    /**
     * 获取用户硬币数统计
     * 
     * @param userId 用户ID
     * @param beginDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 硬币数
     */
    Integer getCoinFromReportCount(Long userId, LocalDateTime beginDateTime, LocalDateTime endDateTime);

    /**
     * 获取用户评论数统计
     * 
     * @param userId 用户ID
     * @param beginDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 评论数
     */
    Integer getCommentFromReportCount(Long userId, LocalDateTime beginDateTime, LocalDateTime endDateTime);

    /**
     * 获取视频总数
     * 
     * @return 视频总数
     */
    Long getVideoCount();

    /**
     * 分页查询待审核视频
     * 
     * @param videoPendingDTO 分页查询条件
     * @return 待审核视频列表
     */
    List<VideoPending> getVideoByPage(VideoPendingDTO videoPendingDTO);

    /**
     * 获取用户所有视频
     * 
     * @param userId 用户ID
     * @return 待审核视频列表
     */
    List<VideoPending> getAllVideoByUserId(Long userId);

    /**
     * 获取视频上传统计
     * 
     * @param beginDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 时间段内上传视频数
     */
    Integer getVideoUploadReport(LocalDateTime beginDateTime, LocalDateTime endDateTime);
}

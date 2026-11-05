package com.yilena.service.dao;

import com.yilena.service.entity.po.UserFollowing;
import com.yilena.service.entity.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 关注关系数据访问接口
 * 
 * 功能说明：
 * 1. 提供关注和取消关注功能
 * 2. 支持更新特别关注
 * 3. 支持获取关注和粉丝列表
 * 4. 支持判断是否已关注
 * 
 */
@Mapper
public interface FollowMapper {
    void addFollow(UserFollowing userFollowing);

    void updateSpecialFollow(UserFollowing userFollowing);

    UserFollowing getById(Long followerId, Long followedId);

    void deleteFollow(UserFollowing userFollowing);

    List<Long> getFollowingIds(Long id);

    UserFollowing isFollow(Long followerId, Long followedId);

    List<UserVO> getFollowingUsers(Long userId);

    List<Long> getFollowedIds(Long userId);
}

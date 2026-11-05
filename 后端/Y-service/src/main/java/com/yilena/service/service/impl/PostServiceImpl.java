package com.yilena.service.service.impl;

import com.alibaba.fastjson.JSON;
import com.yilena.service.constant.*;
import com.yilena.service.dao.FollowMapper;
import com.yilena.service.dao.PostMapper;
import com.yilena.service.dao.UserMapper;
import com.yilena.service.dao.VideoMapper;
import com.yilena.service.entity.PageResult;
import com.yilena.service.entity.dto.PostDTO;
import com.yilena.service.entity.po.History;
import com.yilena.service.entity.po.Post;
import com.yilena.service.entity.po.User;
import com.yilena.service.entity.po.Video;
import com.yilena.service.entity.vo.PostVO;
import com.yilena.service.es.PostES;
import com.yilena.service.service.PostService;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.SensitiveWordFilter;
import com.yilena.service.utils.SnowFlake;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 动态服务实现类
 * 
 * 功能说明：
 * 1. 实现动态（帖子）相关的业务逻辑
 * 2. 处理动态的创建、查询、更新、删除
 * 3. 集成Elasticsearch进行搜索
 * 4. 实现动态与视频的关联
 * 
 * 核心功能：
 * - 动态发布：创建文字或视频动态，过滤敏感词
 * - 动态查询：分页查询、用户动态查询、关注动态查询
 * - 动态管理：更新、删除动态
 * - 数据统计：更新用户动态数量
 * 
 * 技术架构：
 * - 使用RabbitMQ异步同步动态数据到Elasticsearch
 * - 使用敏感词过滤工具保障内容安全
 * - 使用雪花算法生成唯一ID
 * - 使用事务管理确保数据一致性
 * 
 */
@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    /**
     * 动态数据访问对象
     */
    private final PostMapper postMapper;

    /**
     * RabbitMQ消息模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 动态Elasticsearch操作类
     */
    private final PostES postES;

    /**
     * 视频数据访问对象
     */
    private final VideoMapper videoMapper;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 关注数据访问对象
     */
    private final FollowMapper followMapper;

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * 添加动态
     * 
     * 功能：创建新的动态（帖子）
     * 业务逻辑：
     * 1. 初始化动态统计数据（点击、点赞、收藏等）
     * 2. 设置创建和更新时间
     * 3. 生成唯一动态ID
     * 4. 过滤标题和内容中的敏感词
     * 5. 设置动态可见性为公开
     * 6. 将图片和标签列表转换为JSON格式
     * 7. 判断动态类型（文字或视频）
     * 8. 更新用户动态数量
     * 9. 保存动态到数据库
     * 10. 异步同步到Elasticsearch
     * 
     * @param post 动态信息
     */
    @Transactional
    @Override
    public void addPost(Post post) {
        post.setClicks(0);
        post.setLikes(0);
        post.setFavorites(0);
        post.setShares(0);
        post.setComments(0);
        post.setCreatedTime(LocalDateTime.now());
        post.setUpdatedTime(LocalDateTime.now());
        post.setId(snowFlake.getID());

        post.setTitle(SensitiveWordFilter.filterSensitiveWords(post.getTitle()));
        post.setContent(SensitiveWordFilter.filterSensitiveWords(post.getContent()));

        // 不需要后台审核
        post.setVisibility(StatusConstant.STATUS_YES);

        // list转json
        String imageUrlJson = JSON.toJSONString(post.getImageUrl());
        post.setImageUrlJson(imageUrlJson);
        String tagsJson = JSON.toJSONString(post.getTags());
        post.setTagsJson(tagsJson);

        // 判断是文字还是转发视频动态
        if(post.getVideoId() == null){
            post.setPostType(PostConstant.Text_Post);

            // 更新视频转发数
            Video video = videoMapper.getVideoById(post.getVideoId());
            if(video != null){
                video.setShares(video.getShares() + 1);
                videoMapper.updateVideo(video);
            }
        }
        else {
            post.setPostType(PostConstant.Video_Post);
        }

        // 更新用户动态数
        User user = userMapper.getUserById(post.getUserId());
        if(user != null){
            Integer postCount = user.getPostCount();
            user.setPostCount(postCount + 1);
            userMapper.updateUserById(user);
        }

        postMapper.addPost(post);

        PostVO postVO = new PostVO();
        BeanUtils.copyProperties(post, postVO);
        postVO.setType(MqConstant.SEND_ADD);
        rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.POST_BINDING_KEY, postVO);
    }

    /**
     * 分页查询动态
     * 
     * 功能：分页查询动态列表
     * 业务逻辑：
     * 1. 使用Elasticsearch进行搜索
     * 2. 支持分页功能
     * 3. 返回分页结果
     * 
     * @param postDTO 动态查询条件
     * @return 分页查询结果
     */
    @Override
    public PageResult<PostVO> getPostByPage(PostDTO postDTO) {
        return postES.getPostByPage(postDTO);
    }

    /**
     * 根据ID查询动态
     * 
     * 功能：根据动态ID查询动态详细信息
     * 业务逻辑：
     * 1. 查询动态信息
     * 2. 解析图片URL JSON数据
     * 3. 解析标签JSON数据
     * 4. 返回动态实体对象
     * 
     * @param id 动态ID
     * @return 动态实体对象
     */
    @Transactional
    @Override
    public Post getPostById(Long id) {

        Post p = postMapper.getPostById(id);
        String imageUrlJson = p.getImageUrlJson();
        String tagsJson = p.getTagsJson();
        if(imageUrlJson != null) {
            p.setImageUrl(JSON.parseArray(imageUrlJson, String.class));
        }
        p.setTags(JSON.parseArray(tagsJson, String.class));

        //原本以为是动态和专栏是一类东西，后来才发现不是，动态是不需要以下业务逻辑的
//        if(p.getVisibility().equals(StatusConstant.STATUS_YES)){
//            if(!p.getUserId().equals(CurrentHolder.getCurrent())){
//                p.setClicks(p.getClicks() + 1);
//                postMapper.updatePost(p);
//            }
//        }
//
//        if(CurrentHolder.getCurrent() != null) {
//            // 添加到历史记录
//            History history = new History();
//            history.setId(snowFlake.getID());
//            history.setEntityType(EntityConstant.POST_TYPE);
//            history.setEntityId(p.getId());
//            history.setCreatedTime(LocalDateTime.now());
//            rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.HISTORY_BINDING_KEY, history);
//        }

        return p;
    }

    /**
     * 更新动态
     * 
     * 功能：更新动态信息
     * 业务逻辑：
     * 1. 设置更新时间
     * 2. 将图片和标签列表转换为JSON格式
     * 3. 更新动态信息到数据库
     * 4. 异步同步更新到Elasticsearch
     * 
     * @param post 动态更新信息
     */
    @Override
    public void updatePost(Post post) {
        post.setUpdatedTime(LocalDateTime.now());
        String imageUrlJson = JSON.toJSONString(post.getImageUrl());
        post.setImageUrlJson(imageUrlJson);
        String tagsJson = JSON.toJSONString(post.getTags());
        post.setTagsJson(tagsJson);
        postMapper.updatePost(post);

        PostVO postVO = new PostVO();
        BeanUtils.copyProperties(post, postVO);
        postVO.setType(MqConstant.SEND_PUT);
        rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.POST_BINDING_KEY, postVO);
    }

    /**
     * 删除动态
     * 
     * 功能：删除指定动态
     * 业务逻辑：
     * 1. 删除动态记录
     * 2. 更新用户动态数量
     * 3. 异步同步删除操作到Elasticsearch
     * 
     * @param id 动态ID
     */
    @Override
    public void deletePost(Long id) {
        postMapper.deletePost(id);

        // 更新用户动态数
        User user = userMapper.getUserById(CurrentHolder.getCurrent());
        if(user != null){
            Integer postCount = user.getPostCount();
            user.setPostCount(postCount - 1);
            userMapper.updateUserById(user);
        }

        PostVO postVO = new PostVO();
        postVO.setId(id);
        postVO.setType(MqConstant.SEND_DELETE);
        rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.POST_BINDING_KEY, postVO);
    }

    /**
     * 获取关注用户的动态
     * 
     * 功能：获取当前用户关注用户的动态列表
     * 业务逻辑：
     * 1. 获取用户关注列表
     * 2. 添加用户自己到列表
     * 3. 查询这些用户的动态
     * 4. 解析图片和标签JSON数据
     * 5. 填充视频相关信息
     * 6. 返回动态列表
     * 
     * @param userId 用户ID
     * @return 动态列表
     */
    @Override
    public List<PostVO> getPostByFollowing(Long userId) {
        List<Long> followingIds = followMapper.getFollowingIds(userId);
        // 添加自己
        followingIds.add(userId);
        List<PostVO>postVOS =  postMapper.getPostByUserIds(followingIds);
        if(postVOS.isEmpty()){
            return List.of();
        }
        postVOS.forEach(postVO -> {
            String imageUrlJson = postVO.getImageUrlJson();
            postVO.setImageUrl(JSON.parseArray(imageUrlJson, String.class));
            String tagsJson = postVO.getTagsJson();
            postVO.setTags(JSON.parseArray(tagsJson, String.class));

            if(postVO.getVideoId() != null){
                Video video = videoMapper.getVideoById(postVO.getVideoId());
                if(video != null){
                    User u = userMapper.getUserById(video.getUserId());
                    postVO.setVideoUsername(u.getUsername());
                    postVO.setVideoUserId(u.getId());
                    postVO.setVideoUserImage(u.getImage());
                    postVO.setVideoCoverUrl(video.getCoverUrl());
                    postVO.setVideoTitle(video.getTitle());
                    postVO.setVideoDescription(video.getDescription());
                    postVO.setVideoViews(video.getViews());
                    postVO.setVideoBarrages(video.getBarrages());
                    postVO.setVideoTime(video.getTime());
                }
            }
        });
        return postVOS;
    }

    /**
     * 获取我的动态
     * 
     * 功能：获取当前用户的动态列表
     * 业务逻辑：
     * 1. 获取当前用户ID
     * 2. 查询用户动态列表
     * 3. 解析图片和标签JSON数据
     * 4. 填充视频相关信息
     * 5. 返回动态列表
     * 
     * @return 动态列表
     */
    @Override
    public List<PostVO> getMyPosts() {
        Long userId = CurrentHolder.getCurrent();
        List<PostVO> postVOS = postMapper.getMyPost(userId);
        if(postVOS.isEmpty()){
            return List.of();
        }
        postVOS.forEach(postVO -> {
            String imageUrlJson = postVO.getImageUrlJson();
            postVO.setImageUrl(JSON.parseArray(imageUrlJson, String.class));
            String tagsJson = postVO.getTagsJson();
            postVO.setTags(JSON.parseArray(tagsJson, String.class));

            if(postVO.getVideoId() != null){
                Video video = videoMapper.getVideoById(postVO.getVideoId());
                if(video != null){
                    User u = userMapper.getUserById(video.getUserId());
                    postVO.setVideoUsername(u.getUsername());
                    postVO.setVideoUserId(u.getId());
                    postVO.setVideoUserImage(u.getImage());
                    postVO.setVideoCoverUrl(video.getCoverUrl());
                    postVO.setVideoTitle(video.getTitle());
                    postVO.setVideoDescription(video.getDescription());
                    postVO.setVideoViews(video.getViews());
                    postVO.setVideoBarrages(video.getBarrages());
                    postVO.setVideoTime(video.getTime());
                }
            }
        });
        return postVOS;
    }

    /**
     * 根据用户ID获取动态
     * 
     * 功能：获取指定用户的动态列表
     * 业务逻辑：
     * 1. 根据用户ID查询动态列表
     * 2. 解析图片和标签JSON数据
     * 3. 填充视频相关信息
     * 4. 返回动态列表
     * 
     * @param userId 用户ID
     * @return 动态列表
     */
    @Override
    public List<PostVO> getPostByUserId(Long userId) {
        List<PostVO> postVOS  = postMapper.getPostByUserId(userId);
        if(postVOS.isEmpty()){
            return List.of();
        }
        postVOS.forEach(postVO -> {
            String imageUrlJson = postVO.getImageUrlJson();
            postVO.setImageUrl(JSON.parseArray(imageUrlJson, String.class));
            String tagsJson = postVO.getTagsJson();
            postVO.setTags(JSON.parseArray(tagsJson, String.class));

            if(postVO.getVideoId() != null){
                Video video = videoMapper.getVideoById(postVO.getVideoId());
                if(video != null){
                    User u = userMapper.getUserById(video.getUserId());
                    postVO.setVideoUsername(u.getUsername());
                    postVO.setVideoUserId(u.getId());
                    postVO.setVideoUserImage(u.getImage());
                    postVO.setVideoCoverUrl(video.getCoverUrl());
                    postVO.setVideoTitle(video.getTitle());
                    postVO.setVideoDescription(video.getDescription());
                    postVO.setVideoViews(video.getViews());
                    postVO.setVideoBarrages(video.getBarrages());
                    postVO.setVideoTime(video.getTime());
                }
            }
        });
        return postVOS;
    }
}

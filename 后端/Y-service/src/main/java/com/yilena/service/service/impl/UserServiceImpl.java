package com.yilena.service.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yilena.service.constant.*;
import com.yilena.service.dao.FavoritesFolderMapper;
import com.yilena.service.dao.UserMapper;
import com.yilena.service.dao.VideoCollectionMapper;
import com.yilena.service.entity.LoginInfo;
import com.yilena.service.entity.PageResult;
import com.yilena.service.entity.dto.UserDTO;
import com.yilena.service.entity.dto.UserPendingPageQueryDTO;
import com.yilena.service.entity.dto.UserPendingStatusDTO;
import com.yilena.service.entity.po.FavoritesFolder;
import com.yilena.service.entity.po.User;
import com.yilena.service.entity.po.VideoCollection;
import com.yilena.service.entity.vo.UserVO;
import com.yilena.service.es.UserES;
import com.yilena.service.exception.LoginException;
import com.yilena.service.exception.RegisterException;
import com.yilena.service.service.UserService;
import com.yilena.service.utils.JwtUtils;
import com.yilena.service.utils.SnowFlake;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 用户服务实现类
 * 
 * 功能说明：
 * 1. 实现用户相关的业务逻辑
 * 2. 处理用户登录、注册、信息管理
 * 3. 集成Elasticsearch、RabbitMQ、Redis等中间件
 * 4. 实现用户权限和状态管理
 * 
 * 核心功能：
 * - 用户登录认证：密码验证、JWT令牌生成
 * - 用户注册：密码加密、默认数据初始化
 * - 用户信息管理：查询、更新、状态管理
 * - 用户权限管理：账号状态控制、审核处理
 * 
 * 技术架构：
 * - 使用Spring Security进行密码加密
 * - 使用JWT实现无状态认证
 * - 使用RabbitMQ异步同步用户数据到ES
 * - 使用Redis缓存用户信息
 * - 使用PageHelper实现分页查询
 * 
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /**
     * 用户数据访问对象
     */
    private final UserMapper userMapper;

    /**
     * RabbitMQ消息模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 用户Elasticsearch操作类
     */
    private final UserES UserES;

    /**
     * 收藏夹数据访问对象
     */
    private final FavoritesFolderMapper favoritesFolderMapper;

    /**
     * 密码编码器
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 视频合集数据访问对象
     */
    private final VideoCollectionMapper videoCollectionMapper;

    /**
     * Redis模板对象
     */
    private final RedisTemplate redisTemplate;

    /**
     * 用户登录
     * 
     * 功能：验证用户身份，生成JWT令牌
     * 业务逻辑：
     * 1. 验证用户名和密码不为空
     * 2. 根据用户名查询用户信息
     * 3. 检查用户是否存在
     * 4. 检查用户状态是否正常
     * 5. 验证密码是否正确
     * 6. 生成JWT令牌
     * 7. 返回登录信息
     * 
     * @param user 用户登录信息（用户名和密码）
     * @return 登录信息（包含JWT令牌）
     * @throws LoginException 登录失败异常
     */
    @Override
    public LoginInfo login(User user) {
        // 校验用户名密码
        if(user.getUsername() == null || user.getPassword() == null){
            throw new LoginException(UserConstant.USER_EMPTY_FAIL);
        }
        User dbUser = userMapper.getByUsername(user.getUsername());
        // 没找到用户
        if(dbUser == null){
            throw new LoginException(UserConstant.USER_LOGIN_INEXIST_FAIL);
        } else {

            if(dbUser.getStatus().equals(StatusConstant.STATUS_NO)){
                throw new LoginException(UserConstant.USER_CAN_NOT_LOGIN);
            }

            // 密码解密后进行匹配
            if(passwordEncoder.matches(user.getPassword(), dbUser.getPassword())){
                // 生成jwt令牌
                Map<String, Object> claims = Map.of("id",dbUser.getId(),"username", dbUser.getUsername());
                String jwt = JwtUtils.generateToken(claims);
                return new LoginInfo(dbUser.getUsername(), dbUser.getPassword(), jwt);
            } else {
                throw new LoginException(UserConstant.USER_LOGIN_FAIL);
            }
        }
    }

    /**
     * 用户注册
     * 
     * 功能：创建新用户账号，初始化用户数据
     * 业务逻辑：
     * 1. 验证用户名和密码不为空
     * 2. 加密用户密码
     * 3. 设置用户默认值（状态、时间、头像等）
     * 4. 生成唯一用户ID
     * 5. 保存用户到数据库
     * 6. 异步同步用户数据到Elasticsearch
     * 7. 创建默认收藏夹
     * 8. 创建默认视频合集
     * 
     * @param user 用户注册信息
     * @throws RegisterException 注册失败异常
     */
    @Transactional
    @Override
    public void register(User user) {
        // 校验用户名密码
        if(user.getUsername() == null || user.getPassword() == null){
            throw new RegisterException(UserConstant.USER_EMPTY_FAIL);
        }
        // 密码加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // 设置默认值
        user.setStatus(StatusConstant.STATUS_YES);
        user.setUpdatedTime(LocalDateTime.now());
        user.setCreatedTime(LocalDateTime.now());
        user.setPostCount(0);
        user.setVideoCount(0);
        user.setImage("https://yilena-practice-project.oss-cn-beijing.aliyuncs.com/Yilena-video-web/25/04/05-8746f4d1a8404cc5a05c50a4079eda37.jpg");
        user.setFollowersCount(0);
        user.setFollowingCount(0);
        user.setCoins(0);
        user.setDescription("这家伙很懒，还没有简介……");
        user.setIsRecord(StatusConstant.STATUS_YES);

        Long id = snowFlake.getID();
        user.setId(id);
        userMapper.addUser(user);

        // 异步发送消息到队列，添加到es索引库
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setType(MqConstant.SEND_ADD);
        rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.USER_BINDING_KEY, userVO);

        // 新增默认收藏夹
        FavoritesFolder favoritesFolder = new FavoritesFolder();
        favoritesFolder.setId(snowFlake.getID());
        favoritesFolder.setTitle(DefaultConstant.DEFAULT_FAVORITES_TITLE);
        favoritesFolder.setUserId(id);
        favoritesFolder.setVideoIdsJson("[]");
        favoritesFolder.setSubscribersJson("[]");
        favoritesFolder.setVisibility(StatusConstant.STATUS_YES);
        favoritesFolder.setCreatedTime(LocalDateTime.now());
        favoritesFolder.setIsCoverAuto(StatusConstant.STATUS_YES);
        favoritesFolder.setCoverUrl(CoverUrlConstant.NULL);
        favoritesFolder.setVideoCount(0);
        favoritesFolder.setClicks(0);
        favoritesFolder.setUpdatedTime(LocalDateTime.now());
        // 设置为默认
        favoritesFolder.setIsDefault(StatusConstant.STATUS_YES);
        favoritesFolderMapper.addFavoritesFolder(favoritesFolder);

        //新增默认投稿合集
        VideoCollection videoCollection = new VideoCollection();
        videoCollection.setId(snowFlake.getID());
        videoCollection.setTitle(DefaultConstant.DEFAULT_COLLECTION_TITLE);
        videoCollection.setUserId(id);
        videoCollection.setVideoIdsJson("[]");
        videoCollection.setVisibility(StatusConstant.STATUS_YES);
        videoCollection.setIsDefault(StatusConstant.STATUS_YES);
        videoCollection.setCreatedTime(LocalDateTime.now());
        videoCollection.setUpdatedTime(LocalDateTime.now());
        videoCollectionMapper.addVideoCollection(videoCollection);
    }

    /**
     * 分页查询用户
     * 
     * 功能：分页查询用户列表
     * 业务逻辑：
     * 1. 使用Elasticsearch进行全局模糊查询
     * 2. 支持分页功能
     * 3. 返回分页结果
     * 
     * @param userDTO 用户查询条件
     * @return 分页查询结果
     */
    @Override
    public PageResult<User> getUserByPage(UserDTO userDTO) {
        // mysql查询
//        PageHelper.startPage(userDTO.getPage(), userDTO.getPageSize());
//        List<User> users = userMapper.getUserByPage(userDTO);
//        Page<User> p = (Page<User>) users;

        // 全局模糊查询用es，id具体查询再用sql
        return UserES.getUserByPage(userDTO);
    }

    /**
     * 根据ID查询用户
     * 
     * 功能：根据用户ID查询用户详细信息
     * 业务逻辑：
     * 1. 调用Mapper查询用户
     * 2. 返回用户实体对象
     * 
     * @param id 用户ID
     * @return 用户实体对象
     */
    @Override
    public User getUserById(Long id) {
        return userMapper.getUserById(id);
    }

    /**
     * 更新用户信息
     * 
     * 功能：更新用户详细信息
     * 业务逻辑：
     * 1. 验证用户是否存在
     * 2. 删除Redis缓存
     * 3. 处理密码修改逻辑
     * 4. 更新用户信息到数据库
     * 5. 异步同步更新到Elasticsearch
     * 
     * @param user 用户更新信息
     * @throws RuntimeException 用户不存在或密码错误异常
     */
    @Transactional
    @Override
    public void updateUserById(User user) {

        User dbUser = userMapper.getUserById(user.getId());
        if(dbUser == null){
            throw new RuntimeException(UserConstant.USER_LOGIN_INEXIST_FAIL);
        }
        // 删除redis缓存
        redisTemplate.delete("user::" + dbUser.getUsername());
        redisTemplate.delete("user::" + dbUser.getId());

        if(user.getOldPassword() != null){
            if(user.getOldPassword().equals(dbUser.getPassword())){
                throw new RuntimeException(UserConstant.USER_PASSWORD_SAME_FAIL);
            }
            if(passwordEncoder.matches(user.getOldPassword(), dbUser.getPassword())) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            }else{
                throw new RuntimeException(UserConstant.USER_PASSWORD_WRONG);
            }
        }else{
            // 密码为空，不修改
            user.setPassword(null);
        }

        user.setUpdatedTime(LocalDateTime.now());
        userMapper.updateUserById(user);

        // 异步发送消息到队列
        // 这里依旧需要转化成UserVO，因为我们需要展示的数据并没有那么多
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setType(MqConstant.SEND_PUT);
        rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.USER_BINDING_KEY, userVO);
    }

    /**
     * 根据用户名查询用户
     * 
     * 功能：根据用户名查询用户信息
     * 业务逻辑：
     * 1. 调用Mapper查询用户
     * 2. 返回用户实体对象
     * 
     * @param username 用户名
     * @return 用户实体对象
     */
    @Override
    public User getUserByUsername(String username) {
        return userMapper.getByUsername(username);
    }

    /**
     * 更新用户状态
     * 
     * 功能：更新用户账号状态（启用/禁用）
     * 业务逻辑：
     * 1. 根据用户ID查询用户
     * 2. 根据状态值更新用户状态
     * 3. 启用时清除原因，禁用时设置原因
     * 4. 更新用户状态到数据库
     * 5. 异步同步更新到Elasticsearch
     * 
     * @param userPendingStatusDTO 用户状态更新信息
     */
    @Transactional
    @Override
    public void updateUserStatus(UserPendingStatusDTO userPendingStatusDTO) {
        User user = userMapper.getUserById(userPendingStatusDTO.getId());
        if(userPendingStatusDTO.getStatus().equals(StatusConstant.STATUS_YES)){
            user.setStatus(StatusConstant.STATUS_YES);
            // 移除原因
            user.setReason("");
            userMapper.updateUserById(user);
        } else {
            user.setStatus(StatusConstant.STATUS_NO);
            // 设置原因
            user.setReason(userPendingStatusDTO.getReason());
            userMapper.updateUserById(user);
        }

        // 异步发送消息到队列
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        userVO.setType(MqConstant.SEND_PUT);
        rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.USER_BINDING_KEY, userVO);
    }

    /**
     * 获取JWT令牌
     * 
     * 功能：根据用户ID生成JWT令牌
     * 业务逻辑：
     * 1. 根据用户ID查询用户信息
     * 2. 生成JWT令牌
     * 3. 返回登录信息
     * 
     * @param id 用户ID
     * @return 登录信息（包含JWT令牌）
     */
    @Override
    public LoginInfo getToken(Long id) {
        User dbUser = userMapper.getUserById(id);
        // 生成jwt令牌
        Map<String, Object> claims = Map.of("id",dbUser.getId(),"username", dbUser.getUsername());
        String jwt = JwtUtils.generateToken(claims);
        return new LoginInfo(dbUser.getUsername(), dbUser.getPassword(), jwt);
    }

    /**
     * 分页查询待审核用户
     * 
     * 功能：分页查询待审核用户列表
     * 业务逻辑：
     * 1. 设置分页参数
     * 2. 查询待审核用户列表
     * 3. 返回分页结果
     * 
     * @param userPendingPageQueryDTO 分页查询条件
     * @return 分页查询结果
     */
    @Override
    public PageResult<User> getUserPendingByPage(UserPendingPageQueryDTO userPendingPageQueryDTO) {
        PageHelper.startPage(userPendingPageQueryDTO.getPage(), userPendingPageQueryDTO.getPageSize());
        List<User> users = userMapper.getUserPendingByPage(userPendingPageQueryDTO);
        Page<User> p = (Page<User>) users;
        return new PageResult<>(p.getTotal(),p.getResult());
    }
}

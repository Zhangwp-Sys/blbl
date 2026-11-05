package com.yilena.service.dao;

import com.yilena.service.entity.dto.UserPendingPageQueryDTO;
import com.yilena.service.entity.po.User;
import com.yilena.service.entity.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户数据访问接口
 * 
 * 功能说明：
 * 1. 定义用户相关的数据库操作方法
 * 2. 提供用户CRUD操作
 * 3. 支持用户查询和统计
 * 
 * 方法说明：
 * - getByUsername: 根据用户名查询用户
 * - addUser: 添加用户
 * - getUserById: 根据ID查询用户
 * - updateUserById: 更新用户信息
 * - getUserByIds: 批量查询用户
 * - updateIsRecord: 更新历史记录设置
 * - getTotalUser: 获取用户总数
 * - getUserPendingByPage: 分页查询待审核用户
 * - getUserReport: 获取用户注册统计
 * 
 */
@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     * 
     * @param username 用户名
     * @return 用户实体对象
     */
    User getByUsername(String username);

    /**
     * 添加用户
     * 
     * @param user 用户实体对象
     */
    void addUser(User user);

    /**
     * 根据ID查询用户
     * 
     * @param id 用户ID
     * @return 用户实体对象
     */
    User getUserById(Long id);

    /**
     * 更新用户信息
     * 
     * @param user 用户实体对象
     */
    void updateUserById(User user);

    /**
     * 批量查询用户
     * 
     * @param ids 用户ID列表
     * @param username 用户名（可选，用于模糊查询）
     * @return 用户VO列表
     */
    List<UserVO> getUserByIds(List<Long> ids, String username);

    /**
     * 更新历史记录设置
     * 
     * @param userId 用户ID
     * @param isRecord 是否记录历史（0:记录，1:不记录）
     */
    void updateIsRecord(Long userId, Integer isRecord);

    /**
     * 获取用户总数
     * 
     * @return 用户总数
     */
    Long getTotalUser();

    /**
     * 分页查询待审核用户
     * 
     * @param userPendingPageQueryDTO 分页查询条件
     * @return 待审核用户列表
     */
    List<User> getUserPendingByPage(UserPendingPageQueryDTO userPendingPageQueryDTO);

    /**
     * 获取用户注册统计
     * 
     * @param beginDateTime 开始时间
     * @param endDateTime 结束时间
     * @return 时间段内注册用户数
     */
    Integer getUserReport(LocalDateTime beginDateTime, LocalDateTime endDateTime);
}

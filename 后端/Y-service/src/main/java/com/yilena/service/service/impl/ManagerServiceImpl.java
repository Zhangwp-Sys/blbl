package com.yilena.service.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yilena.service.constant.StatusConstant;
import com.yilena.service.constant.UserConstant;
import com.yilena.service.dao.ManagerMapper;
import com.yilena.service.entity.LoginInfo;
import com.yilena.service.entity.PageResult;
import com.yilena.service.entity.dto.ManagerPageQueryDTO;
import com.yilena.service.entity.po.Manager;
import com.yilena.service.exception.LoginException;
import com.yilena.service.service.ManagerService;
import com.yilena.service.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.aspectj.weaver.NameMangler;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 管理员服务实现类
 * 
 * 功能说明：
 * 1. 实现管理员相关的业务逻辑
 * 2. 处理管理员登录、信息管理
 * 3. 实现管理员账号管理
 * 
 * 核心功能：
 * - 管理员登录：验证身份，生成JWT令牌
 * - 管理员信息管理：查询、更新信息
 * - 管理员账号管理：更新密码、状态管理
 * - 分页查询：支持分页查询管理员列表
 * 
 */
@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    /**
     * 管理员数据访问对象
     */
    private final ManagerMapper managerMapper;

    /**
     * 管理员登录
     * 
     * 功能：验证管理员身份，生成JWT令牌
     * 业务逻辑：
     * 1. 验证用户名和密码不为空，否则抛出 USER_EMPTY_FAIL 异常
     * 2. 根据用户名查询管理员信息，不存在则抛出 USER_LOGIN_INEXIST_FAIL 异常
     * 3. 检查管理员状态是否正常（status != 0），已被冻结则抛出 USER_CAN_NOT_LOGIN 异常
     * 4. 比对密码是否一致，不一致则抛出 USER_LOGIN_FAIL 异常
     * 5. 密码验证通过后，生成JWT令牌并返回
     * 
     * @param manager 管理员登录信息
     * @return 登录信息（包含JWT令牌）
     * @throws LoginException 当用户名或密码为空、用户不存在、用户被冻结或密码错误时抛出
     */
    @Override
    public LoginInfo login(Manager manager) {
        // 校验用户名密码
        if(manager.getUsername() == null || manager.getPassword() == null){
            throw new LoginException(UserConstant.USER_EMPTY_FAIL);
        }
        Manager dbManager = managerMapper.getByUsername(manager.getUsername());
        // 没找到用户
        if(dbManager == null){
            throw new LoginException(UserConstant.USER_LOGIN_INEXIST_FAIL);
        } else {

            if(dbManager.getStatus().equals(StatusConstant.STATUS_NO)){
                throw new LoginException(UserConstant.USER_CAN_NOT_LOGIN);
            }

            // 密码解密后进行匹配
            if(manager.getPassword().equals(dbManager.getPassword())){
                // 生成jwt令牌
                Map<String, Object> claims = Map.of("id", dbManager.getId(),"username", dbManager.getUsername());
                String jwt = JwtUtils.generateToken(claims);
                return new LoginInfo(dbManager.getUsername(),null, jwt);
            } else {
                throw new LoginException(UserConstant.USER_LOGIN_FAIL);
            }
        }
    }

    /**
     * 更新管理员状态
     * 
     * @param status 状态
     * @param id 管理员ID
     * @return void
     */
    @Override
    public void updateStatus(Integer status, Integer id) {
        managerMapper.updateStatus(status, id);
    }

    /**
     * 根据ID获取管理员信息
     * 
     * @param id 管理员ID
     * @return 管理员信息
     */
    @Override
    public Manager getManagerById(Integer id) {
        return managerMapper.getManagerById(id);
    }

    /**
     * 更新管理员密码
     * 
     * 功能：更新管理员密码
     * 业务逻辑：
     * 1. 验证管理员是否存在，不存在则抛出 USER_LOGIN_INEXIST_FAIL 异常
     * 2. 验证新密码与旧密码是否相同，相同则抛出 USER_PASSWORD_SAME_FAIL 异常
     * 3. 验证旧密码是否正确，不正确则抛出 USER_PASSWORD_WRONG 异常
     * 4. 更新密码
     * 
     * @param manager 管理员密码更新信息
     * @return void
     * @throws LoginException 当管理员不存在、新密码与旧密码相同或原密码错误时抛出
     */
    @Override
    public void updatePassword(Manager manager) {
        Manager dbManager = managerMapper.getManagerById(manager.getId());
        if(dbManager == null){
            throw new LoginException(UserConstant.USER_LOGIN_INEXIST_FAIL);
        }
        if(manager.getNewPassword().equals(dbManager.getPassword())){
            throw new LoginException(UserConstant.USER_PASSWORD_SAME_FAIL);
        }
        if(manager.getOldPassword().equals(dbManager.getPassword())){
            managerMapper.updatePassword(manager);
        } else {
            throw new LoginException(UserConstant.USER_PASSWORD_WRONG);
        }
    }

    /**
     * 根据用户名获取管理员信息
     * 
     * @param username 用户名
     * @return 管理员信息
     */
    @Override
    public Manager getByUsername(String username) {
        return managerMapper.getByUsername(username);
    }

    /**
     * 分页查询管理员列表
     * 
     * @param managerPageQueryDTO 分页查询条件
     * @return 管理员分页结果
     */
    @Override
    public PageResult<Manager> getManagerByPage(ManagerPageQueryDTO managerPageQueryDTO) {
        PageHelper.startPage(managerPageQueryDTO.getPage(), managerPageQueryDTO.getPageSize());
        List<Manager> managers = managerMapper.getManagerByPage(managerPageQueryDTO);
        Page<Manager> p = (Page<Manager>)managers;
        return new PageResult<>(p.getTotal(), p.getResult());
    }
}

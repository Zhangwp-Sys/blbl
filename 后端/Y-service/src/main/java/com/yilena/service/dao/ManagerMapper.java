package com.yilena.service.dao;

import com.yilena.service.entity.dto.ManagerPageQueryDTO;
import com.yilena.service.entity.po.Manager;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 管理员数据访问接口
 * 
 * 功能说明：
 * 1. 提供管理员账号的增删改查
 * 2. 支持根据用户名查询管理员
 * 3. 支持分页查询管理员列表
 * 
 */
@Mapper
public interface ManagerMapper {
    Manager getByUsername(String username);

    void updateStatus(Integer status, Integer id);

    Manager getManagerById(Integer id);

    void updatePassword(Manager manager);

    List<Manager> getManagerByPage(ManagerPageQueryDTO managerPageQueryDTO);
}

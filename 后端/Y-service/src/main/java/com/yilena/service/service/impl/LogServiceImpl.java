package com.yilena.service.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.yilena.service.constant.LogTypeConstant;
import com.yilena.service.dao.ManagerLogMapper;
import com.yilena.service.dao.UserLogMapper;
import com.yilena.service.entity.PageResult;
import com.yilena.service.entity.dto.LogPageQueryDTO;
import com.yilena.service.entity.po.Log;
import com.yilena.service.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 日志服务实现类
 * 
 * 功能说明：
 * 1. 实现日志查询相关的业务逻辑
 * 2. 支持用户日志和管理员日志的分页查询
 * 
 */
@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {

    /**
     * 管理员日志数据访问对象
     */
    private final ManagerLogMapper managerLogMapper;

    /**
     * 用户日志数据访问对象
     */
    private final UserLogMapper userLogMapper;

    /**
     * 分页查询日志
     * 
     * 功能：根据日志类型分页查询日志
     * 业务逻辑：
     * 1. 设置分页参数
     * 2. 根据日志类型选择对应的Mapper
     * 3. 返回分页结果
     * 
     * @param logPageQueryDTO 分页查询条件
     * @return 日志分页结果
     */
    @Override
    public PageResult<Log> getLogByPage(LogPageQueryDTO logPageQueryDTO) {
        PageHelper.startPage(logPageQueryDTO.getPage(), logPageQueryDTO.getPageSize());
        List<Log> logs = null;
        if(logPageQueryDTO.getType().equals(LogTypeConstant.USER_LOG)){
            logs = userLogMapper.getLogByPage(logPageQueryDTO);
        }else if(logPageQueryDTO.getType().equals(LogTypeConstant.MANAGER_LOG)){
            logs = managerLogMapper.getLogByPage(logPageQueryDTO);
        }
        Page<Log> p = (Page<Log>) logs;
        return new PageResult<>(p.getTotal(), p.getResult());
    }
}

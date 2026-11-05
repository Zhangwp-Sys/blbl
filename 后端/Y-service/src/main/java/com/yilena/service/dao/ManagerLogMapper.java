package com.yilena.service.dao;

import com.yilena.service.entity.dto.LogPageQueryDTO;
import com.yilena.service.entity.po.Log;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 管理员操作日志数据访问接口
 * 
 * 功能说明：
 * 1. 提供管理员操作日志的增删查
 * 2. 支持分页查询日志
 * 3. 支持删除半年前的过期日志
 * 
 */
@Mapper
public interface ManagerLogMapper {
    void addLog(Log newLog);

    List<Log> getLogByPage(LogPageQueryDTO logPageQueryDTO);

    void deleteLogsBeforeHalfYear();
}

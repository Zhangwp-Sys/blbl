package com.yilena.service.dao;

import com.yilena.service.entity.dto.LogPageQueryDTO;
import com.yilena.service.entity.po.Log;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户操作日志数据访问接口
 * 
 * 功能说明：
 * 1. 提供用户操作日志的增删查
 * 2. 支持分页查询日志
 * 3. 支持删除半年前的过期日志
 * 
 */
@Mapper
public interface UserLogMapper {

    void addLog(Log operateLog);

    List<Log> getLogByPage(LogPageQueryDTO logPageQueryDTO);

    void deleteLogsBeforeHalfYear();
}

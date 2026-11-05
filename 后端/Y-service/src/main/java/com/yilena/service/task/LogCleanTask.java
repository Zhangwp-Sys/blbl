package com.yilena.service.task;
import com.yilena.service.dao.HotSearchMapper;
import com.yilena.service.dao.ManagerLogMapper;
import com.yilena.service.dao.UserLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 日志清理定时任务
 * 
 * 功能说明：
 * 1. 定期清理历史日志数据
 * 2. 释放数据库存储空间
 * 3. 保持系统性能
 * 
 * 任务配置：
 * - 执行时间：每天凌晨2点
 * - 清理范围：半年前的日志数据
 * - 执行频率：每天一次
 * 
 * 清理策略：
 * - 用户操作日志：删除半年前的记录
 * - 管理员操作日志：删除半年前的记录
 * 
 * 使用场景：
 * - 系统维护
 * - 数据库优化
 * - 存储空间管理
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Component
@EnableScheduling
@Slf4j
@RequiredArgsConstructor
public class LogCleanTask {

    /**
     * 用户日志Mapper
     */
    private final UserLogMapper userLogMapper;

    /**
     * 管理员日志Mapper
     */
    private final ManagerLogMapper managerLogMapper;

    /**
     * 执行日志清理任务
     * 
     * 功能：清理半年前的日志数据
     * 执行时间：每天凌晨2点
     * 业务逻辑：
     * 1. 记录任务开始日志
     * 2. 清理用户操作日志
     * 3. 清理管理员操作日志
     * 4. 记录任务完成日志
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void executeEveryMinute() {
        try {
            log.info("清除历史记录定时任务开始执行，当前时间: {}", System.currentTimeMillis());

            userLogMapper.deleteLogsBeforeHalfYear();
            managerLogMapper.deleteLogsBeforeHalfYear();

            log.info("清除历史记录定时任务执行完成");
        } catch (Exception e) {
            log.error("清除历史记录定时任务执行出错: {}", e.getMessage());
            e.printStackTrace();
        }
    }
}

package com.yilena.service.aop;

import com.yilena.service.dao.ManagerLogMapper;
import com.yilena.service.dao.UserLogMapper;
import com.yilena.service.entity.po.Log;
import com.yilena.service.log.LogOperation;
import com.yilena.service.utils.CurrentHolder;
import com.yilena.service.utils.SnowFlake;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 日志切面类
 * 
 * 功能说明：
 * 1. 使用AOP实现操作日志记录
 * 2. 拦截@LogOperation注解的方法
 * 3. 记录方法执行时间和参数
 * 4. 分类存储用户和管理员操作日志
 * 
 * 切面逻辑：
 * - 环绕通知：记录方法执行前后的时间
 * - 参数记录：记录方法名、参数和返回值
 * - 性能监控：计算方法执行耗时
 * - 分类存储：根据包名区分用户和管理员操作
 * 
 * 使用场景：
 * - 用户操作日志记录
 * - 管理员操作日志记录
 * - 系统性能监控
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    /**
     * 用户日志Mapper
     */
    private final UserLogMapper userLogMapper;

    /**
     * 管理员日志Mapper
     */
    private final ManagerLogMapper managerLogMapper;

    /**
     * 雪花算法ID生成器
     */
    private final SnowFlake snowFlake;

    /**
     * 环绕通知：记录操作日志
     * 
     * 功能：拦截@LogOperation注解的方法，记录操作日志
     * 业务逻辑：
     * 1. 记录方法开始时间
     * 2. 执行目标方法
     * 3. 计算方法执行耗时
     * 4. 构建日志对象
     * 5. 根据包名分类存储日志
     * 
     * @param joinPoint 连接点
     * @param olog 日志操作注解
     * @return 方法执行结果
     * @throws Throwable 方法执行异常
     */
    @Around("@annotation(olog)")
    public Object around(ProceedingJoinPoint joinPoint, LogOperation olog) throws Throwable {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        // 执行方法
        Object result = joinPoint.proceed();
        // 当前时间
        long endTime = System.currentTimeMillis();
        // 耗时
        long costTime = endTime - startTime;

        // 构建日志对象
        Log newLog = new Log();
        newLog.setId(snowFlake.getID());
        newLog.setOperateUserId(getCurrentUserId());
        newLog.setOperateTime(LocalDateTime.now());
        newLog.setMethodName(joinPoint.getSignature().getName());
        newLog.setMethodParams(Arrays.toString(joinPoint.getArgs()));
        newLog.setReturnValue(result.toString());
        newLog.setCostTime(costTime / 1000.0);

        String classname = joinPoint.getTarget().getClass().getName().split("\\.")[5];
        if(joinPoint.getTarget().getClass().getName().split("\\.")[4].equals("user")) {
            switch (classname) {
                case "CommentController" -> newLog.setClassName("评论操作");
                case "FavoritesFolderController" -> newLog.setClassName("收藏夹操作");
                case "PostController" -> newLog.setClassName("动态操作");
                case "UserController" -> newLog.setClassName("用户操作");
                case "UserVideoPendingController" -> newLog.setClassName("用户视频后台操作");
                case "VideoCollectionController" -> newLog.setClassName("视频投稿合集操作");
                case "VideoController" -> newLog.setClassName("视频操作");
            }
            userLogMapper.addLog(newLog);
        }else{
            switch (classname) {
                case "ManagerController" -> newLog.setClassName("管理员操作");
                case "PostPendingController" -> newLog.setClassName("动态审核操作");
                case "UserPendingController" -> newLog.setClassName("用户审核操作");
                case "VideoPendingController" -> newLog.setClassName("视频审核操作");
            }
            managerLogMapper.addLog(newLog);
        }
        log.info("操作日志：{}", newLog);
        return result;
    }

    /**
     * 获取当前用户ID
     * 
     * @return 当前用户ID
     */
    private Long getCurrentUserId() {
        return CurrentHolder.getCurrent();
    }
}
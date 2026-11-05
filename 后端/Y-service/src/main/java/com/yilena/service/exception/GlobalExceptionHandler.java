package com.yilena.service.exception;

import com.yilena.service.entity.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 * 
 * 功能说明：
 * 1. 捕获并处理所有Controller层抛出的异常
 * 2. 统一异常响应格式
 * 3. 记录异常日志信息
 * 4. 提供友好的错误提示
 * 
 * 异常处理策略：
 * - LoginException: 登录异常处理
 * - RegisterException: 注册异常处理
 * - EsException: Elasticsearch异常处理
 * - SQLIntegrityConstraintViolationException: 数据库完整性约束异常
 * - DuplicateKeyException: 重复键异常
 * - RuntimeException: 运行时异常处理
 * 
 * 使用场景：
 * - 用户登录失败
 * - 用户名已存在
 * - 数据库操作异常
 * - 系统运行时异常
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理登录异常
     * 
     * @param e 登录异常
     * @return 错误结果
     */
    @ExceptionHandler(LoginException.class)
    public Result handleException(LoginException e) {
        log.error("******异常信息******   ", e);
        return Result.error(e.getMessage());
    }

    /**
     * 处理注册异常
     * 
     * @param e 注册异常
     * @return 错误结果
     */
    @ExceptionHandler(RegisterException.class)
    public Result handleException(RegisterException e) {
        log.error("******异常信息******   ", e);
        return Result.error(e.getMessage());
    }

    /**
     * 处理Elasticsearch异常
     * 
     * @param e Elasticsearch异常
     * @return 错误结果
     */
    @ExceptionHandler(EsException.class)
    public Result handleException(EsException e) {
        log.error("******异常信息******   ", e);
        return Result.error(e.getMessage());
    }

    /**
     * 处理数据库完整性约束异常
     * 
     * @param e 数据库完整性约束异常
     * @return 错误结果
     */
    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public Result handleException(SQLIntegrityConstraintViolationException e) {
        log.error("******异常信息******   ", e);
        if (e.getMessage().contains("Duplicate entry")) {
            String[] split = e.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return Result.error(msg);
        }
        return Result.error("操作失败");
    }

    /**
     * 处理重复键异常
     * 
     * @param e 重复键异常
     * @return 错误结果
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public Result handleException(DuplicateKeyException e) {
        log.error("******异常信息******   ", e);
        if (e.getMessage().contains("Duplicate entry")) {
            String[] split = e.getMessage().split(" ");
            String msg = split[9] + "已存在";
            return Result.error(msg);
        }
        return Result.error("操作失败");
    }

    /**
     * 处理运行时异常
     * 
     * @param e 运行时异常
     * @return 错误结果
     */
    @ExceptionHandler(RuntimeException.class)
    public Result handleException(RuntimeException e) {
        log.error("******异常信息******   ", e);
        return Result.error(e.getMessage());
    }
}

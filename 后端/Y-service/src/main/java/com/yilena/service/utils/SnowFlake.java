package com.yilena.service.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 雪花算法ID生成器
 * 
 * 功能说明：
 * 1. 生成分布式唯一ID
 * 2. 基于雪花算法实现
 * 3. 支持高并发场景
 * 
 * 雪花算法结构（64位）：
 * - 1位符号位（固定为0）
 * - 41位时间戳（毫秒级，可用69年）
 * - 10位机器ID（5位数据中心ID + 5位工作机器ID）
 * - 12位序列号（每毫秒最多生成4096个ID）
 * 
 * 优势：
 * - 全局唯一ID
 * - 趋势递增
 * - 高性能（单机每秒可生成约400万个ID）
 * - 无依赖，不占用网络资源
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Component
public class SnowFlake {

    /**
     * 雪花算法配置属性
     */
    private SnowFlakeProperties properties;

    /**
     * 默认构造函数
     */
    public SnowFlake() {
    }

    /**
     * 带配置的构造函数
     * 
     * @param properties 雪花算法配置属性
     */
    public SnowFlake(SnowFlakeProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成唯一ID
     * 
     * 功能：基于雪花算法生成分布式唯一ID
     * 业务逻辑：
     * 1. 获取当前时间戳
     * 2. 处理时钟回拨异常
     * 3. 处理同一毫秒内的序列号
     * 4. 组装64位ID
     * 
     * @return 生成的唯一ID
     * @throws RuntimeException 时钟回拨异常
     */
    public synchronized  long getID(){
        //获得当前时间
        long now = System.currentTimeMillis();
        long lastTimeStamp = properties.getLastTimeStamp();
        //当前系统时间小于上一次记录时间
        if (now < lastTimeStamp){
            throw new RuntimeException("时钟回拨异常");
        }
        //相同时间 要序列号进制增量
        if (now == lastTimeStamp){
            //防止溢出
            long sequence = properties.getSequence();
            sequence = (sequence + 1) & properties.getSequenceMask();
            //更新sequence 的值
            properties.setSequence(sequence);
            if (sequence == 0L){
                //溢出处理
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                //获取下一毫秒时间 （有锁）
                now = System.currentTimeMillis();
            }
        }else {
            //置0
            properties.setSequence(0L);
        }
        //记录当前时间
        properties.setLastTimeStamp(now);
        return ((now - properties.getStartTimeStamp()) << properties.getTimeStampIndex()) |
                (properties.getDataCenterID() << properties.getDataCenterIndex())|
                (properties.getWorkID() << properties.getWorkIDIndex()) | properties.getSequence();
    }
}
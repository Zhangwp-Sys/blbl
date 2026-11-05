package com.yilena.service.service.impl;

import com.yilena.service.constant.MqConstant;
import com.yilena.service.dao.HotSearchMapper;
import com.yilena.service.entity.po.HotSearch;
import com.yilena.service.mq.HotSearchQueueListener;
import com.yilena.service.service.HotSearchService;
import com.yilena.service.utils.SnowFlake;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 热搜服务实现类
 * 
 * 功能说明：
 * 1. 实现热搜相关的业务逻辑
 * 2. 获取热搜视频列表
 * 3. 处理搜索历史记录
 * 
 * 核心功能：
 * - 热搜查询：获取热搜视频列表
 * - 搜索历史：通过RabbitMQ异步添加搜索历史
 * 
 */
@Service
@RequiredArgsConstructor
public class HotSearchServiceImpl implements HotSearchService {

    /**
     * 热搜数据访问对象
     */
    private final HotSearchMapper hotSearchMapper;

    /**
     * RabbitMQ消息模板
     */
    private final RabbitTemplate rabbitTemplate;

    /**
     * 获取热搜视频列表
     * 
     * @return 热搜视频列表
     */
    @Override
    public List<HotSearch> getTopHotSearch() {
        return hotSearchMapper.getHotSearchVideo();
    }

    /**
     * 添加搜索历史
     * 
     * 功能：通过RabbitMQ异步添加搜索历史
     * 
     * @param hotSearch 热搜信息
     * @return void
     */
    @Override
    public void addSearchHistory(HotSearch hotSearch) {
        //mq发送消息
        rabbitTemplate.convertAndSend(MqConstant.CRUD_EXCHANGE, MqConstant.HOT_SEARCH_BINDING_KEY, hotSearch);
    }
}

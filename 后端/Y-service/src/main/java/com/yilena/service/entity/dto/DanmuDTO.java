package com.yilena.service.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 弹幕查询数据传输对象
 * 
 * 功能说明：
 * 1. 封装弹幕查询请求参数
 * 2. 支持按视频ID和当前播放时间查询
 * 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DanmuDTO {
    private Long videoId;
    private double currentTime;
    private Integer pageSize;
}

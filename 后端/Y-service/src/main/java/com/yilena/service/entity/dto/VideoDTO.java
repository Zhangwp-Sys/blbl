package com.yilena.service.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 视频数据传输对象
 * 
 * 功能说明：
 * 1. 封装视频查询/操作请求参数
 * 2. 包含视频基本信息和查询条件
 * 
 * 字段说明：
 * - id: 视频ID
 * - title: 视频标题
 * - description: 视频描述
 * - tags: 视频标签列表
 * - username: 用户名
 * - beginDate/endDate: 日期范围查询
 * - beginTime/endTime: 时间范围查询
 * - sortType: 排序方式
 * - page/pageSize: 分页参数
 * 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoDTO {
    private Long id;
    private String description;
    private String title;
    private List<String> tags;
    private String username;
    private LocalDateTime beginDate;
    private LocalDateTime endDate;
    private Long beginTime;
    private Long endTime;
    private Integer sortType;
    private Integer page;
    private Integer pageSize;
}

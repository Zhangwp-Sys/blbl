package com.yilena.service.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论查询数据传输对象
 * 
 * 功能说明：
 * 1. 封装评论查询请求参数
 * 2. 支持按视频标题和排序方式查询
 * 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentQueryDTO {
    private String videoTitle;
    private Integer sortType;
}

package com.yilena.service.entity.dto;

import com.yilena.service.entity.vo.PostVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 动态数据传输对象
 * 
 * 功能说明：
 * 1. 封装动态查询/操作请求参数
 * 2. 包含动态基本信息和分页参数
 * 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostDTO {
    private Long id;
    private String title;
    private String content;
    private List<String> tags;
    private String username;
    private Integer page;
    private Integer pageSize;
}

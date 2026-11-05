package com.yilena.service.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户数据传输对象
 * 
 * 功能说明：
 * 1. 封装用户查询请求参数
 * 2. 包含用户ID、用户名和分页参数
 * 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private Integer sortType;
    private Integer Page;
    private Integer PageSize;
}

package com.yilena.service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 * 
 * 功能说明：
 * 1. 封装分页查询结果
 * 2. 包含总记录数和当前页数据
 * 
 * @param <T> 数据类型泛型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> implements Serializable{
    private Long total;
    private List<T> rows;
}

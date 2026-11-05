package com.yilena.service.entity.vo;

import com.yilena.service.constant.StatusConstant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.io.Serializable;

/**
 * 用户视图对象
 * 
 * 功能说明：
 * 1. 封装用户完整信息返回给前端
 * 2. 包含用户基本信息、统计数据和状态信息
 * 
 * 字段说明：
 * - id: 用户ID
 * - username: 用户名
 * - description: 用户描述
 * - image: 用户头像
 * - videoCount: 视频数
 * - followingCount: 关注数
 * - followersCount: 粉丝数
 * - status: 用户状态
 * - type: 用户类型
 * - unReadMessageCount: 未读消息数
 * 
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserVO  implements Serializable {
    private Long id;
    @Field(type = FieldType.Keyword)
    private String username;
    private String description;
    private Integer videoCount;
    private String image;
    private Integer followingCount;
    private Integer followersCount;
    private Integer status;
    private Integer type;

    private Long unReadMessageCount;
}

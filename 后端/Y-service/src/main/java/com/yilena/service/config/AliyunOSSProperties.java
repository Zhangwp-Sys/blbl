package com.yilena.service.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云OSS配置属性类
 * 
 * 功能说明：
 * 1. 绑定阿里云OSS相关配置属性
 * 2. 提供OSS连接和访问所需的配置信息
 * 
 * 配置项：
 * - aliyun.oss.endpoint: OSS访问端点
 * - aliyun.oss.bucketName: OSS存储桶名称
 * - aliyun.oss.region: OSS区域
 * - aliyun.oss.accessKeyId: 访问密钥ID
 * - aliyun.oss.accessKeySecret: 访问密钥密钥
 * 
 * 使用场景：
 * - 文件上传到OSS
 * - 生成文件访问URL
 * - 管理OSS存储资源
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOSSProperties {

    /**
     * OSS访问端点
     * 例如：oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;

    /**
     * OSS存储桶名称
     * 全球唯一
     */
    private String bucketName;

    /**
     * OSS区域
     * 例如：cn-hangzhou
     */
    private String region;

    /**
     * 访问密钥ID
     * 用于身份验证
     */
    private String accessKeyId;

    /**
     * 访问密钥密钥
     * 用于身份验证
     */
    private String accessKeySecret;
}

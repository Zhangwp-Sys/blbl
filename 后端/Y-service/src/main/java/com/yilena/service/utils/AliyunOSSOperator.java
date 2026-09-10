package com.yilena.service.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.yilena.service.config.AliyunOSSProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

// 这个类的作用：提供阿里云OSS的上传功能
@Component
@RequiredArgsConstructor
public class AliyunOSSOperator {

    private final AliyunOSSProperties aliyunOSSProperties;

    /** 生成一个唯一的 Bucket 名称 */
    public static String generateUniqueName(String prefix) {
        // 获取当前时间
        LocalDate timestamp = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/MM/dd");
        UUID uuid = UUID.randomUUID();
        // 连接以形成一个唯一的 Bucket 名称
        return "Yilena-video-web/" + timestamp.format(formatter) + "-" + uuid.toString().replace("-", "") + prefix;
    }

    public String upload(byte[] content, String objectName) throws com.aliyuncs.exceptions.ClientException {
        // 设置 OSS Endpoint 和 Bucket 名称
        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucketName();
        String region = aliyunOSSProperties.getRegion();
        String accessKeyId = aliyunOSSProperties.getAccessKeyId();
        String accessKeySecret = aliyunOSSProperties.getAccessKeySecret();

        // 创建 OSSClient 实例
        CredentialsProvider credentialsProvider = CredentialsProviderFactory.newDefaultCredentialProvider(accessKeyId, accessKeySecret);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();
        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content));
            System.out.println("2. 文件 " + objectName + " 上传成功。");
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        // 构建访问URL，兼容endpoint带或不带协议头的情况
        String host = endpoint.contains("//") ? endpoint.split("//")[1] : endpoint;
        return "https://" + bucketName + "." + host + "/" + objectName;
    }
}

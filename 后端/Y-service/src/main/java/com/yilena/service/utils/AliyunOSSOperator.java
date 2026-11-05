package com.yilena.service.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.yilena.service.config.AliyunOSSProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 阿里云OSS操作工具类
 * 
 * 功能说明：
 * 1. 提供文件上传到阿里云OSS的功能
 * 2. 生成唯一的文件名
 * 3. 管理OSS连接和资源释放
 * 
 * 使用场景：
 * - 用户头像上传
 * - 视频文件上传
 * - 其他文件资源上传
 * 
 * 技术实现：
 * - 使用阿里云OSS SDK
 * - 支持断点续传
 * - 自动生成唯一文件名，避免重名
 * 
 * @author Yilena
 * @since 2024-01-01
 * @version 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AliyunOSSOperator {

    /**
     * 阿里云OSS配置属性
     */
    private final AliyunOSSProperties aliyunOSSProperties;

    /**
     * 生成唯一的文件名
     * 
     * 功能：生成包含时间戳和UUID的唯一文件名
     * 命名规则：Yilena-video-web/yy/MM/dd-uuid前缀
     * 
     * @param prefix 文件名前缀
     * @return 生成的唯一文件名
     */
    public static String generateUniqueName(String prefix) {
        // 获取当前时间
        LocalDate timestamp = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yy/MM/dd");
        UUID uuid = UUID.randomUUID();
        // 连接以形成一个唯一的 Bucket 名称
        return "Yilena-video-web/" + timestamp.format(formatter) + "-" + uuid.toString().replace("-", "") + prefix;
    }

    /**
     * 上传文件到阿里云OSS
     * 
     * 功能：将文件内容上传到指定的OSS Bucket
     * 业务逻辑：
     * 1. 创建OSS客户端连接
     * 2. 上传文件内容
     * 3. 构建访问URL
     * 4. 释放资源
     * 
     * @param content 文件内容字节数组
     * @param objectName 对象名称（文件路径）
     * @return 文件访问URL
     * @throws com.aliyuncs.exceptions.ClientException OSS客户端异常
     */
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

    /**
     * 初始化分片上传
     *
     * 功能：向OSS申请分片上传任务
     * 业务逻辑：
     * 1. 创建OSS客户端
     * 2. 发起分片上传初始化请求
     * 3. 返回OSS服务端生成的uploadId（后续分片上传与合并的凭证）
     *
     * @param objectName 对象名称（文件路径）
     * @return uploadId 分片上传任务ID
     */
    public String initMultipartUpload(String objectName) {
        OSS ossClient = buildOssClient();
        try {
            InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(
                    aliyunOSSProperties.getBucketName(), objectName);
            InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
            log.info("分片上传初始化成功：objectName={}, uploadId={}", objectName, result.getUploadId());
            return result.getUploadId();
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 上传单个分片
     *
     * 功能：将一个分片数据流式上传到OSS
     * 业务逻辑：
     * 1. 构建分片上传请求（uploadId + 分片编号 + 数据流 + 分片大小）
     * 2. 流式传输，不将整个文件读入内存，防止OOM
     * 3. 返回OSS为该分片生成的ETag（合并时用于校验完整性）
     *
     * @param objectName  对象名称
     * @param uploadId    分片上传任务ID
     * @param partNumber  分片编号（从1开始）
     * @param inputStream 分片数据流
     * @param partSize    分片大小（字节）
     * @return ETag 该分片的校验值
     */
    public String uploadPart(String objectName, String uploadId, int partNumber,
                             InputStream inputStream, long partSize) {
        OSS ossClient = buildOssClient();
        try {
            UploadPartRequest request = new UploadPartRequest();
            request.setBucketName(aliyunOSSProperties.getBucketName());
            request.setKey(objectName);
            request.setUploadId(uploadId);
            request.setPartNumber(partNumber);
            request.setInputStream(inputStream);
            request.setPartSize(partSize);
            UploadPartResult result = ossClient.uploadPart(request);
            String eTag = result.getPartETag().getETag();
            log.info("分片上传成功：uploadId={}, partNumber={}, ETag={}", uploadId, partNumber, eTag);
            return eTag;
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 合并分片
     *
     * 功能：将所有已上传分片在OSS服务端合并为完整文件
     * 业务逻辑：
     * 1. 提交分片编号与ETag列表（OSS会校验每个分片的ETag是否匹配）
     * 2. 合并操作在OSS服务端完成，不占用应用服务器内存
     * 3. 返回文件访问URL
     *
     * @param objectName 对象名称
     * @param uploadId   分片上传任务ID
     * @param partETags  分片编号与ETag列表（需按分片编号升序）
     * @return 文件访问URL
     */
    public String completeMultipartUpload(String objectName, String uploadId, List<PartETag> partETags) {
        OSS ossClient = buildOssClient();
        try {
            CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(
                    aliyunOSSProperties.getBucketName(), objectName, uploadId, partETags);
            ossClient.completeMultipartUpload(request);
            log.info("分片合并成功：objectName={}, uploadId={}, 分片数={}", objectName, uploadId, partETags.size());
            return buildUrl(objectName);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 取消分片上传
     *
     * 功能：中止分片上传任务，释放OSS服务端已上传的分片
     *
     * @param objectName 对象名称
     * @param uploadId   分片上传任务ID
     */
    public void abortMultipartUpload(String objectName, String uploadId) {
        OSS ossClient = buildOssClient();
        try {
            ossClient.abortMultipartUpload(new AbortMultipartUploadRequest(
                    aliyunOSSProperties.getBucketName(), objectName, uploadId));
            log.info("分片上传已取消：objectName={}, uploadId={}", objectName, uploadId);
        } finally {
            ossClient.shutdown();
        }
    }

    /**
     * 构建OSS客户端实例
     */
    private OSS buildOssClient() {
        CredentialsProvider credentialsProvider = CredentialsProviderFactory.newDefaultCredentialProvider(
                aliyunOSSProperties.getAccessKeyId(), aliyunOSSProperties.getAccessKeySecret());
        return OSSClientBuilder.create()
                .endpoint(aliyunOSSProperties.getEndpoint())
                .credentialsProvider(credentialsProvider)
                .region(aliyunOSSProperties.getRegion())
                .build();
    }

    /**
     * 构建文件访问URL，兼容endpoint带或不带协议头的情况
     */
    private String buildUrl(String objectName) {
        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucketName();
        String host = endpoint.contains("//") ? endpoint.split("//")[1] : endpoint;
        return "https://" + bucketName + "." + host + "/" + objectName;
    }
}

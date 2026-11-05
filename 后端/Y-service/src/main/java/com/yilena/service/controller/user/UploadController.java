package com.yilena.service.controller.user;

import com.aliyun.oss.model.PartETag;
import com.yilena.service.log.LogOperation;
import com.yilena.service.entity.Result;
import com.yilena.service.utils.AliyunOSSOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 文件上传控制器
 * 
 * 功能说明：
 * 1. 提供小文件直传功能（头像、封面等）
 * 2. 提供大文件分片上传功能（视频等），支持断点续传
 * 3. 使用阿里云OSS进行文件存储
 * 4. 返回文件访问URL
 * 
 * 接口列表：
 * - POST /upload              小文件直传（整个文件读入内存，仅适用于头像/封面等小文件）
 * - POST /upload/init         初始化分片上传，返回uploadId
 * - POST /upload/part         上传单个分片（流式传输，不占用应用内存）
 * - POST /upload/merge        合并分片（OSS服务端完成）
 * - GET  /upload/status/{id}  查询已上传分片列表（断点续传）
 * 
 * 分片上传Redis键设计：
 * - upload:{uploadId}  String  objectName（24小时过期）
 * - parts:{uploadId}   Set     "分片编号:ETag"（24小时过期，Set自动去重防止重复记录）
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class UploadController {

    private final AliyunOSSOperator aliyunOSSOperator;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 小文件直传（仅适用于头像、封面等小文件；大视频请走分片上传接口，避免OOM）
     */
    @PostMapping("/upload")
    public Result upload(MultipartFile file) throws Exception {
        // 记录文件上传日志
        log.info("文件上传：{}", file.getOriginalFilename());
        // 检查文件是否为空
        if (!file.isEmpty()) {
            // 获取文件原始名称
            String originalFilename = file.getOriginalFilename();
            // 获取文件扩展名
            String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
            // 调用阿里云OSS操作器上传文件，并获取文件的访问URL
            String url = aliyunOSSOperator.upload(file.getBytes(), AliyunOSSOperator.generateUniqueName(extName));
            // 返回文件上传成功的结果，包含文件的访问URL
            return Result.success(url);
        }
        // 如果文件为空，返回上传失败的结果
        return Result.error("上传失败");
    }

    /**
     * 初始化分片上传
     *
     * 功能：为大文件上传创建分片上传任务
     * 业务逻辑：
     * 1. 生成唯一objectName（时间戳+UUID，避免重名覆盖）
     * 2. 向OSS申请分片上传，获取uploadId
     * 3. Redis记录uploadId与objectName的映射（24小时过期，支持断点续传）
     *
     * @param params 包含fileName（原始文件名）
     * @return uploadId与objectName
     */
    @PostMapping("/upload/init")
    public Result initUpload(@RequestBody Map<String, String> params) {
        String fileName = params.get("fileName");
        if (fileName == null || !fileName.contains(".")) {
            return Result.error("文件名不合法");
        }
        String extName = fileName.substring(fileName.lastIndexOf("."));
        String objectName = AliyunOSSOperator.generateUniqueName(extName);

        // 向OSS申请分片上传任务
        String uploadId = aliyunOSSOperator.initMultipartUpload(objectName);

        // Redis记录uploadId -> objectName（24小时过期，断点续传的凭证）
        redisTemplate.opsForValue().set("upload:" + uploadId, objectName, 24, TimeUnit.HOURS);

        log.info("初始化分片上传：fileName={}, uploadId={}", fileName, uploadId);
        Map<String, String> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("objectName", objectName);
        return Result.success(result);
    }

    /**
     * 上传单个分片
     *
     * 功能：流式上传一个分片到OSS，并记录分片状态到Redis
     * 业务逻辑：
     * 1. 从Redis获取objectName（以服务端记录为准，不信任前端传参）
     * 2. 流式上传分片到OSS（getInputStream，不整读内存），获取ETag
     * 3. Redis Set记录"分片编号:ETag"（Set自动去重，同一分片重复上传不会记录两次）
     *
     * @param uploadId   分片上传任务ID
     * @param partNumber 分片编号（从1开始）
     * @param file       分片数据
     * @return 操作结果
     */
    @PostMapping("/upload/part")
    public Result uploadPart(@RequestParam String uploadId,
                             @RequestParam Integer partNumber,
                             @RequestParam("file") MultipartFile file) throws Exception {
        Object objectName = redisTemplate.opsForValue().get("upload:" + uploadId);
        if (objectName == null) {
            return Result.error("uploadId不存在或已过期，请重新初始化");
        }

        // 流式上传分片，不将整个文件读入内存
        String eTag = aliyunOSSOperator.uploadPart(objectName.toString(), uploadId,
                partNumber, file.getInputStream(), file.getSize());

        // Redis Set记录分片信息（自动去重）
        String key = "parts:" + uploadId;
        redisTemplate.opsForSet().add(key, partNumber + ":" + eTag);
        redisTemplate.expire(key, 24, TimeUnit.HOURS);

        log.info("分片上传成功：uploadId={}, partNumber={}", uploadId, partNumber);
        return Result.success();
    }

    /**
     * 合并分片
     *
     * 功能：将所有已上传分片合并为完整文件
     * 业务逻辑：
     * 1. 从Redis读取已上传分片信息（Set类型）
     * 2. 按分片编号升序排序，构建PartETag列表（合并时必须按顺序传给OSS校验）
     * 3. 调用OSS合并接口（合并操作在OSS服务端完成，不占用应用服务器内存）
     * 4. 清理Redis中的分片状态
     *
     * @param params 包含uploadId
     * @return 文件访问URL
     */
    @PostMapping("/upload/merge")
    public Result mergeParts(@RequestBody Map<String, String> params) {
        String uploadId = params.get("uploadId");
        Object objectName = redisTemplate.opsForValue().get("upload:" + uploadId);
        if (objectName == null) {
            return Result.error("uploadId不存在或已过期");
        }

        // 从Redis获取已上传的分片信息
        Set<Object> partInfos = redisTemplate.opsForSet().members("parts:" + uploadId);
        if (partInfos == null || partInfos.isEmpty()) {
            return Result.error("没有已上传的分片");
        }

        // 按分片编号排序，构建PartETag列表
        List<PartETag> partETags = partInfos.stream()
                .map(info -> {
                    String[] parts = info.toString().split(":");
                    return new PartETag(Integer.parseInt(parts[0]), parts[1]);
                })
                .sorted(Comparator.comparingInt(PartETag::getPartNumber))
                .collect(Collectors.toList());

        // 调用OSS合并接口（服务端完成合并）
        String url = aliyunOSSOperator.completeMultipartUpload(objectName.toString(), uploadId, partETags);

        // 清理Redis中的分片状态
        redisTemplate.delete("upload:" + uploadId);
        redisTemplate.delete("parts:" + uploadId);

        log.info("分片合并完成：uploadId={}, url={}", uploadId, url);
        return Result.success(url);
    }

    /**
     * 查询已上传分片列表
     *
     * 功能：断点续传时，前端先调用本接口查询已上传分片，只重传缺失的分片
     *
     * @param uploadId 分片上传任务ID
     * @return 已上传分片编号列表（升序）
     */
    @GetMapping("/upload/status/{uploadId}")
    public Result uploadStatus(@PathVariable String uploadId) {
        Set<Object> partInfos = redisTemplate.opsForSet().members("parts:" + uploadId);
        List<Integer> uploadedParts = partInfos == null ? Collections.emptyList() :
                partInfos.stream()
                        .map(info -> Integer.parseInt(info.toString().split(":")[0]))
                        .sorted()
                        .collect(Collectors.toList());
        return Result.success(uploadedParts);
    }
}

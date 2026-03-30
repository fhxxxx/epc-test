package com.envision.bunny.module.aws.application;

import com.envision.bunny.infrastructure.aws.AwsProperties;
import com.envision.bunny.module.aws.application.dto.UploadPartDTO;
import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AWS S3操作服务类
 * <p>
 * 该类负责AWS S3的文件上传、下载、删除等操作
 * 支持基础操作、批量操作、元数据操作、预签名URL、分片上传等
 *
 * @author example
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class S3Service {
    private static final Logger logger = LoggerFactory.getLogger(S3Service.class);

    /**
     * S3模板，用于简化的S3操作
     */
    @Autowired(required = false)
    private S3Template s3Template;

    /**
     * S3客户端，用于高级操作
     */
    @Autowired(required = false)
    private S3Client s3Client;

    /**
     * AWS配置属性
     */
    private final AwsProperties awsProperties;

    @Autowired(required = false)
    private S3TransferManager s3TransferManager;

    @Autowired
    private SseService sseService;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);


    /**
     * 上传文件到S3
     * <p>
     * 使用S3TransferManager上传文件，支持进度监听和同步等待完成
     * 自动生成路径和检测Content-Type
     * <p>
     * 技术说明：
     * - 通过创建临时文件来使用S3TransferManager的uploadFile方法
     * - 支持大文件自动分片上传
     * - 使用LoggingTransferListener监听上传进度
     * - 通过completionFuture().join()同步等待上传完成
     *
     * @param file 上传的文件（MultipartFile）
     * @return 上传的文件key
     * @throws IOException 文件读取异常
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();
        String key = "test/" + file.getOriginalFilename();
        String contentType = determineContentType(file.getOriginalFilename(), file.getContentType());

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("upload-", "-" + file.getOriginalFilename());
            file.transferTo(tempFile);

            LoggingTransferListener listener = new LoggingTransferListener(file.getSize());

            UploadFileRequest uploadRequest =
                    UploadFileRequest.builder()
                            .putObjectRequest(req -> req
                                    .bucket(bucketName)
                                    .key(key)
                                    .contentType(contentType))
                            .source(tempFile)
                            .addTransferListener(listener)
                            .build();

            FileUpload upload = s3TransferManager.uploadFile(uploadRequest);

            upload.completionFuture().join();

            return key;
        } catch (Exception e) {
            logger.error("上传文件到S3失败: {}", e.getMessage(), e);
            throw new IOException("上传文件失败: " + e.getMessage(), e);
        } finally {
            if (tempFile != null) {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    /**
     * 异步上传文件到S3，并通过SSE推送进度
     *
     * @param file 上传的文件
     * @return SseEmitter 实例，用于推送进度
     */
    public SseEmitter uploadFileWithProgress(MultipartFile file) {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();
        String key = "test/" + file.getOriginalFilename();
        String contentType = determineContentType(file.getOriginalFilename(), file.getContentType());
        long fileSize = file.getSize();

        SseEmitter emitter = sseService.createConnection();

        CompletableFuture.runAsync(() -> {
            try {
                ProgressTransferListener listener = new ProgressTransferListener(fileSize, sseService);
                AsyncRequestBody asyncRequestBody = AsyncRequestBody.fromInputStream(file.getInputStream(),
                        file.getSize(), executor);

                UploadRequest uploadRequest = UploadRequest.builder()
                        .putObjectRequest(builder -> builder.bucket(bucketName).key(key).contentType(contentType))
                        .requestBody(asyncRequestBody)
                        .addTransferListener(listener)
                        .build();
                Upload upload = s3TransferManager.upload(uploadRequest);

                upload.completionFuture().join();
                sseService.complete();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return emitter;
    }

    /**
     * 确定文件的Content-Type
     * <p>
     * 优先级：MultipartFile.getContentType() > Files.probeContentType() > 默认值
     *
     * @param filename             文件名
     * @param multipartContentType MultipartFile提供的Content-Type
     * @return Content-Type字符串
     */
    private String determineContentType(String filename, String multipartContentType) {
        if (multipartContentType != null && !multipartContentType.isEmpty()) {
            return multipartContentType;
        }

        try {
            String detectedType = Files.probeContentType(Paths.get(filename));
            if (detectedType != null && !detectedType.isEmpty()) {
                return detectedType;
            }
        } catch (IOException e) {
            logger.debug("无法通过文件名检测Content-Type: {}", e.getMessage());
        }

        return "application/octet-stream";
    }

    /**
     * 从S3下载文件
     * <p>
     * 使用S3Template下载文件到HttpServletResponse
     * 支持大文件流式传输，不占用过多内存
     *
     * @param key      文件键（路径）
     * @param response HttpServletResponse对象
     * @throws IOException IO异常
     */
    public void downloadFile(String key, HttpServletResponse response) throws IOException {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();
        S3Resource s3Resource = s3Template.download(bucketName, key);

        String encodedFilename = URLEncoder.encode(s3Resource.getFilename(), StandardCharsets.UTF_8)
                .replace("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedFilename);

        try (InputStream in = s3Resource.getInputStream();
             OutputStream out = response.getOutputStream()) {
            in.transferTo(out);
        }
    }

    /**
     * 从S3删除文件
     * <p>
     * 使用S3Template删除单个文件
     *
     * @param key 文件键（路径）
     * @return 删除结果
     */
    public String deleteFile(String key) {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();
        s3Template.deleteObject(bucketName, key);
        return String.format("文件已删除: %s", key);
    }

    /**
     * 列出S3文件
     * <p>
     * 使用S3Client列出指定前缀的文件
     *
     * @param prefix  前缀（可选）
     * @param maxKeys 最大返回数量（可选）
     * @return 文件列表
     */
    public Map<String, String> listFiles(String prefix, Integer maxKeys) {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();

        Map<String, String> result = new HashMap<>();
        ListObjectsV2Request.Builder requestBuilder = ListObjectsV2Request.builder()
                .bucket(bucketName);

        if (prefix != null && !prefix.isEmpty()) {
            requestBuilder.prefix(prefix);
        }

        if (maxKeys != null && maxKeys > 0) {
            requestBuilder.maxKeys(maxKeys);
        }

        ListObjectsV2Response response = s3Client.listObjectsV2(requestBuilder.build());

        int index = 1;
        for (S3Object s3Object : response.contents()) {
            result.put(String.valueOf(index++), String.format("%s (大小: %d bytes)",
                    s3Object.key(), s3Object.size()));
        }

        return result;
    }

    /**
     * 上传带元数据的文件到S3
     * <p>
     * 使用S3Client上传文件并指定自定义元数据
     * 注意：S3Template的ObjectMetadata不支持Map类型的metadata，需使用S3Client
     *
     * @param key          文件键（路径）
     * @param content      文件内容
     * @param contentType  内容类型
     * @param metadata     自定义元数据
     * @param storageClass 存储类别（可选）
     * @return 上传结果
     */
    public String uploadFileWithMetadata(String key, String content, String contentType,
                                         Map<String, String> metadata, StorageClass storageClass) {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();

        PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key);

        if (contentType != null && !contentType.isEmpty()) {
            requestBuilder.contentType(contentType);
        }

        if (metadata != null && !metadata.isEmpty()) {
            requestBuilder.metadata(metadata);
        }

        if (storageClass != null) {
            requestBuilder.storageClass(storageClass);
        }

        s3Client.putObject(requestBuilder.build(),
                RequestBody.fromBytes(content.getBytes(StandardCharsets.UTF_8)));

        int metadataSize = (metadata != null) ? metadata.size() : 0;
        String storageClassInfo = (storageClass != null) ? storageClass.toString() : "默认";
        return String.format("带元数据文件已上传: %s, 元数据数量: %d, 存储类别: %s",
                key, metadataSize, storageClassInfo);
    }

    /**
     * 生成预签名URL
     * <p>
     * 使用S3Presigner生成临时的文件访问URL
     * 用于分享文件或临时下载
     *
     * @param key        文件键（路径）
     * @param expiration 过期时间（秒）
     * @return 预签名URL
     */
    public String generatePresignedUrl(String key, Integer expiration) {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();
        URL signedGetURL = s3Template.createSignedGetURL(bucketName, key, Duration.ofSeconds(expiration));
        return signedGetURL.toString();
    }

    /**
     * 检查S3文件是否存在
     * <p>
     * 使用S3Template检查对象是否存在
     *
     * @param key 文件键（路径）
     * @return 检查结果
     */
    public boolean checkFileExists(String key) {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();
        return s3Template.objectExists(bucketName, key);
    }

    /**
     * 复制S3文件
     * <p>
     * 在同一个Bucket内复制文件
     * 注意：S3Template不支持直接复制操作，使用S3Client实现
     *
     * @param sourceKey      源文件键
     * @param destinationKey 目标文件键
     * @return 复制结果
     */
    public String copyFile(String sourceKey, String destinationKey, StorageClass storageClass) {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();
        CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .sourceKey(sourceKey)
                .destinationBucket(bucketName)
                .destinationKey(destinationKey)
                .storageClass(storageClass)
                .build();

        s3Client.copyObject(copyRequest);
        return String.format("文件已复制: %s -> %s", sourceKey, destinationKey);
    }

    /**
     * 获取S3文件信息
     * <p>
     * 获取对象的元数据和大小信息
     *
     * @param key 文件键（路径）
     * @return 文件信息
     */
    public Map<String, String> getFileInfo(String key) {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();

        Map<String, String> result = new HashMap<>();
        result.put("key", key);

        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            HeadObjectResponse response = s3Client.headObject(request);
            result.put("size", String.valueOf(response.contentLength()));
            result.put("contentType", response.contentType());
            result.put("storage", response.storageClassAsString());

            // 打印自定义元数据
            Map<String, String> metadata = response.metadata();
            if (metadata != null && !metadata.isEmpty()) {
                result.put("metadata", metadata.toString());
            }

            // 打印恢复状态信息
            if (response.restore() != null) {
                result.put("restoreStatus", response.restore().toString());
            }
        } catch (Exception e) {
            result.put("status", "error");
            result.put("error", e.getMessage());
        }
        return result;
    }

    /**
     * 恢复归档文件
     * <p>
     * 从GLACIER或DEEP_ARCHIVE存储类别恢复文件
     * 恢复后的文件可临时访问指定天数
     * <p>
     * 技术说明：
     * - GLACIER文件恢复通常需要3-5小时
     * - DEEP_ARCHIVE文件恢复通常需要12-48小时
     * - 恢复期间无法下载文件，需等待恢复完成
     * - 恢复完成后文件会临时恢复到可访问状态
     *
     * @param key  文件键（路径）
     * @param days 恢复天数（可选，默认为1天）
     * @return 恢复结果
     */
    public String restoreObject(String key, Integer days) {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();

        RestoreObjectRequest restoreRequest = RestoreObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .restoreRequest(x -> x.days(days))//召回多长时间
                .build();

        s3Client.restoreObject(restoreRequest);

        return String.format("归档文件恢复请求已提交: %s, 恢复天数: %d", key, days);
    }


    /**
     * 初始化上传
     */
    public String initMultipartUpload(String key) {
        String bucketName = awsProperties.getS3().getBasicBucket().getName();
        CreateMultipartUploadRequest request =
                CreateMultipartUploadRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build();

        CreateMultipartUploadResponse response =
                s3Client.createMultipartUpload(request);
        return response.uploadId();
    }

    /**
     * 上传分片
     */
    public CompletedPart uploadPart(
            String key,
            String uploadId,
            int partNumber,
            InputStream inputStream,
            long size
    ) {

        String bucketName = awsProperties.getS3().getBasicBucket().getName();
        UploadPartRequest request =
                UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength(size)
                        .build();

        UploadPartResponse response =
                s3Client.uploadPart(
                        request,
                        RequestBody.fromInputStream(inputStream, size)
                );

        return CompletedPart.builder()
                .partNumber(partNumber)
                .eTag(response.eTag())
                .build();
    }

    /**
     * 完成上传
     */
    public void completeUpload(
            String key,
            String uploadId,
            List<UploadPartDTO> parts
    ) {
        String bucket = awsProperties.getS3().getBasicBucket().getName();
        List<CompletedPart> completedParts = parts.stream()
                .map(p -> CompletedPart.builder()
                        .partNumber(p.getPartNumber())
                        .eTag(p.getETag())
                        .build())
                .toList();

        CompletedMultipartUpload completedMultipartUpload =
                CompletedMultipartUpload.builder()
                        .parts(completedParts)
                        .build();

        CompleteMultipartUploadRequest request =
                CompleteMultipartUploadRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .multipartUpload(completedMultipartUpload)
                        .build();

        s3Client.completeMultipartUpload(request);
    }
}

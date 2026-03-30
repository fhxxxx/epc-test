package com.envision.bunny.module.aws.web;

import com.envision.bunny.module.aws.application.S3Service;
import com.envision.bunny.module.aws.application.dto.UploadPartDTO;
import com.envision.bunny.module.aws.application.dto.s3.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * S3文件操作控制器
 * <p>
 * 负责AWS S3文件上传、下载、删除等操作测试接口
 *
 * @author example
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/test/s3")
public class S3Controller {

    /**
     * 注入S3操作服务
     * 该服务负责S3的文件上传、下载、删除等操作
     */
    @Autowired(required = false)
    private S3Service s3Service;

    /**
     * 上传文件到S3
     * <p>
     * 该接口使用multipart/form-data上传文件到S3
     * 文件路径自动生成为：test/原始文件名
     * Content-Type自动检测：优先使用客户端传递的类型，否则根据文件扩展名检测，失败则使用默认值
     * <p>
     * 使用示例：
     * POST /api/test/s3/upload
     * Content-Type: multipart/form-data
     * Body: file=<文件>
     *
     * @param file 上传的文件
     * @return 上传结果描述信息
     */
    @PostMapping("/upload")
    public String uploadFile(@RequestPart("file") MultipartFile file) throws IOException {
        return s3Service.uploadFile(file);
    }

    /**
     * 上传文件到S3，并实时推送上传进度（SSE）
     * <p>
     * 该接口使用Server-Sent Events (SSE)实时推送上传进度
     * 客户端可以通过SSE流接收进度更新，包括百分比、已传输字节数等
     *
     * @param file 上传的文件
     * @return SseEmitter 实例，用于推送进度
     */
    @PostMapping(value = "/upload-with-progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter uploadFileWithProgress(@RequestPart("file") MultipartFile file) {
        return s3Service.uploadFileWithProgress(file);
    }

    /**
     * 从S3下载文件
     * <p>
     * 该接口下载指定路径的文件，直接输出到响应流
     * 支持大文件流式传输
     * <p>
     * 使用示例：
     * POST /api/test/s3/download
     * {
     *   "key": "test/file.txt"
     * }
     *
     * @param request   下载文件请求对象
     * @param response HttpServletResponse对象
     * @throws IOException IO异常
     */
    @PostMapping("/download")
    public void downloadFile(@RequestBody DownloadFileRequest request, HttpServletResponse response) throws IOException {
        s3Service.downloadFile(request.getKey(), response);
    }

    /**
     * 从S3删除文件
     * @param request 删除文件请求对象
     * @return 删除结果描述信息
     */
    @PostMapping("/delete")
    public String deleteFile(@RequestBody DeleteFileRequest request) {
        return s3Service.deleteFile(request.getKey());
    }

    /**
     * 列出S3文件
     * <p>
     * 该接口列出指定前缀的所有文件
     * <p>
     * 使用示例：
     * POST /api/test/s3/list
     * {
     *   "prefix": "test/",
     *   "maxKeys": 10
     * }
     *
     * @param request 列出文件请求对象
     * @return 文件列表
     */
    @PostMapping("/list")
    public Map<String, String> listFiles(@RequestBody ListFilesRequest request) {
        return s3Service.listFiles(request.getPrefix(), request.getMaxKeys());
    }

    /**
     * 上传带元数据的文件到S3
     *
     * @param request 上传带元数据文件请求对象
     * @return 上传结果描述信息
     */
    @PostMapping("/upload-with-metadata")
    public String uploadFileWithMetadata(@RequestBody UploadFileWithMetadataRequest request) {
        return s3Service.uploadFileWithMetadata(
                request.getKey(),
                request.getContent(),
                request.getContentType(),
                request.getMetadata(),
                request.getStorageClass()
        );
    }

    /**
     * 生成预签名URL
     * <p>
     * 该接口生成临时的文件访问URL
     * 用于分享文件或临时下载，无需公开访问权限
     *
     * @param request 生成预签名URL请求对象
     * @return 预签名URL
     */
    @PostMapping("/presigned-url")
    public String generatePresignedUrl(@RequestBody GeneratePresignedUrlRequest request) {
        return s3Service.generatePresignedUrl(request.getKey(), request.getExpiration());
    }

    /**
     * 检查S3文件是否存在
     *
     * @param request 检查文件存在请求对象
     * @return 检查结果
     */
    @PostMapping("/exists")
    public boolean checkFileExists(@RequestBody CheckFileExistsRequest request) {
        return s3Service.checkFileExists(request.getKey());
    }

    /**
     * 复制S3文件
     *
     * @param request 复制文件请求对象
     * @return 复制结果描述信息
     */
    @PostMapping("/copy")
    public String copyFile(@RequestBody CopyFileRequest request) {
        return s3Service.copyFile(request.getSourceKey(), request.getDestinationKey(), request.getStorageClass());
    }

    /**
     * 获取S3文件信息
     * <p>
     * 该接口获取文件的元数据和大小信息
     * <p>
     * 使用示例：
     * POST /api/test/s3/info
     * {
     *   "key": "test/file.txt"
     * }
     *
     * @param request 获取文件信息请求对象
     * @return 文件信息
     */
    @PostMapping("/info")
    public Map<String, String> getFileInfo(@RequestBody GetFileInfoRequest request) {
        return s3Service.getFileInfo(request.getKey());
    }

    /**
     * 召回文件
     * <p>
     * 该接口从GLACIER或DEEP_ARCHIVE存储类别恢复文件
     *
     * @param request 恢复文件请求对象
     * @return 恢复结果描述信息
     */
    @PostMapping("/restore")
    public String restoreObject(@RequestBody RestoreFileRequest request) {
        return s3Service.restoreObject(request.getKey(), request.getDays());
    }

    /**
     * 初始化分片上传
     * <p>
     * 该接口初始化分片上传，返回上传ID和文件名
     *
     * @param filename 文件名
     * @return 初始化结果
     */
    @PostMapping("/multipart/init")
    public Map<String, String> init(@RequestParam String filename) {

        String key = "multipart/" + filename;

        String uploadId =
                s3Service.initMultipartUpload(key);

        Map<String, String> result = new HashMap<>();
        result.put("uploadId", uploadId);
        result.put("key", key);

        return result;
    }

    /**
     * 上传分片
     */
    @PostMapping("/multipart/upload")
    public UploadPartDTO uploadPart(
            @RequestParam String key,
            @RequestParam String uploadId,
            @RequestParam int partNumber,
            @RequestParam MultipartFile file
    ) throws IOException {

        CompletedPart part = s3Service.uploadPart(
                key,
                uploadId,
                partNumber,
                file.getInputStream(),
                file.getSize()
        );

        return new UploadPartDTO(
                part.partNumber(),
                part.eTag()
        );
    }


    /**
     * 完成分片上传
     */
    @PostMapping("/multipart/complete")
    public String complete(
            @RequestParam String key,
            @RequestParam String uploadId,
            @RequestBody List<UploadPartDTO> parts
    ) {

        s3Service.completeUpload(
                key,
                uploadId,
                parts
        );

        return "upload success";
    }
}

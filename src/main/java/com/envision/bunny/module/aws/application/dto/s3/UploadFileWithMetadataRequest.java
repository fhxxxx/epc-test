package com.envision.bunny.module.aws.application.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.s3.model.StorageClass;

import java.util.Map;

/**
 * 上传带元数据文件请求对象
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadFileWithMetadataRequest {

    /**
     * 文件键（路径）
     */
    private String key;

    /**
     * 文件内容
     */
    private String content;

    /**
     * 内容类型
     */
    private String contentType;

    /**
     * 自定义元数据
     */
    private Map<String, String> metadata;

    /**
     * 存储类别（可选）
     * 可选值: STANDARD, STANDARD_IA, ONEZONE_IA, GLACIER, DEEP_ARCHIVE
     * 不传则使用默认存储类别
     */
    private StorageClass storageClass;
}

package com.envision.bunny.module.aws.application.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.services.s3.model.StorageClass;

/**
 * 复制文件请求对象
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopyFileRequest {

    /**
     * 源文件键
     */
    private String sourceKey;

    /**
     * 目标文件键
     */
    private String destinationKey;

    private StorageClass storageClass;
}

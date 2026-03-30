package com.envision.bunny.module.aws.application.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 生成预签名URL请求对象
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePresignedUrlRequest {

    /**
     * 文件键（路径）
     */
    private String key;

    /**
     * 过期时间（秒）
     */
    private Integer expiration;
}

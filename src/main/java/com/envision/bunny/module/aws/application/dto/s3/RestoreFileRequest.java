package com.envision.bunny.module.aws.application.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 恢复归档文件请求对象
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestoreFileRequest {

    /**
     * 文件键（路径）
     */
    private String key;

    /**
     * 恢复天数（可选，默认为1天）
     * 文件恢复后可临时访问的天数
     */
    private Integer days;
}

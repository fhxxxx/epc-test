package com.envision.bunny.module.aws.application.dto.s3;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列出文件请求对象
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListFilesRequest {

    /**
     * 前缀（可选）
     */
    private String prefix;

    /**
     * 最大返回数量（可选）
     */
    private Integer maxKeys;
}

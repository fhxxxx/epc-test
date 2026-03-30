package com.envision.bunny.module.aws.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上传进度数据模型
 * <p>
 * 用于 SSE 推送文件上传进度信息
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadProgress {
    /**
     * 进度百分比 (0-100)
     */
    private int percent;

    /**
     * 已传输字节数
     */
    private long transferredBytes;

    /**
     * 总字节数
     */
    private long totalBytes;
}

package com.envision.bunny.module.aws.application;

import com.envision.bunny.module.aws.domain.UploadProgress;
import software.amazon.awssdk.transfer.s3.progress.TransferListener;

/**
 * 进度传输监听器
 * <p>
 * 监听 S3 文件上传进度，并通过 SSE 推送到客户端
 *
 * @author example
 * @version 1.0.0
 */
public class ProgressTransferListener implements TransferListener {

    private final long totalBytes;
    private final SseService sseService;

    /**
     * 构造方法
     *
     * @param totalBytes   文件总字节数
     * @param sseService  SSE 服务
     */
    public ProgressTransferListener(long totalBytes, SseService sseService) {
        this.totalBytes = totalBytes;
        this.sseService = sseService;
    }

    @Override
    public void bytesTransferred(Context.BytesTransferred context) {
        long current = context.progressSnapshot().transferredBytes();
        int percent = (int) ((current * 100) / totalBytes);
        UploadProgress progress = new UploadProgress(
            percent,
            current,
            totalBytes
        );
        sseService.sendProgress(progress);
    }

    @Override
    public void transferInitiated(Context.TransferInitiated context) {
    }

    @Override
    public void transferComplete(Context.TransferComplete context) {
    }

    @Override
    public void transferFailed(Context.TransferFailed context) {
    }
}

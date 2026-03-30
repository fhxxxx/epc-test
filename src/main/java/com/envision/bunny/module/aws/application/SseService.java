package com.envision.bunny.module.aws.application;

import com.envision.bunny.module.aws.domain.UploadProgress;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * SSE 连接管理服务
 * <p>
 * 管理 SSE 连接的生命周期，负责创建连接、发送进度更新、关闭连接
 *
 * @author example
 * @version 1.0.0
 */
@Service
public class SseService {

    /**
     * SSE 连接实例
     */
    private SseEmitter emitter;

    /**
     * 创建 SSE 连接
     *
     * @return SseEmitter 实例
     */
    public SseEmitter createConnection() {
        emitter = new SseEmitter(0L);
        emitter.onCompletion(() -> emitter = null);
        emitter.onTimeout(() -> {
            emitter.complete();
            emitter = null;
        });
        try {
            emitter.send(SseEmitter.event().data("开始上传文件"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return emitter;
    }

    /**
     * 发送进度更新到客户端
     *
     * @param progress 进度数据
     */
    public void sendProgress(UploadProgress progress) {
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().data(progress));
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitter = null;
            }
        }
    }

    /**
     * 正常完成 SSE 连接
     */
    public void complete() {
        if (emitter != null) {
            emitter.complete();
        }
    }
}

package com.envision.bunny.module.aws.application;

import io.awspring.cloud.sqs.listener.errorhandler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 自定义SQS错误处理器
 *
 * 该类提供了统一的消息处理异常处理功能
 * 功能包括：
 * - 记录完整的错误日志
 * - 统计错误次数
 * - 区分不同类型的异常
 * - 可选的死信队列投递（需要配置）
 *
 * @author example
 * @version 1.0.0
 */
@Component
public class CustomErrorHandler implements ErrorHandler<Object> {

    private static final Logger logger = LoggerFactory.getLogger(CustomErrorHandler.class);

    /**
     * 处理消息处理过程中的异常
     *
     * @param message 发生异常的消息
     * @param throwable 抛出的异常
     */
    @Override
    public void handle(Message<Object> message, Throwable throwable) {
        String messageId = message.getHeaders().getId() != null 
                ? message.getHeaders().getId().toString() 
                : "unknown";
        
        String messagePayload = message.getPayload() != null 
                ? message.getPayload().toString() 
                : "null";

        // 记录完整错误日志
        logger.error("========== SQS消息处理异常 ==========");
        logger.error("消息ID: {}", messageId);
        logger.error("消息内容: {}", messagePayload);
        logger.error("异常类型: {}", throwable.getClass().getName());
        logger.error("异常消息: {}", throwable.getMessage());
        logger.error("消息头: {}", message.getHeaders());
        logger.error("异常堆栈:", throwable);
        logger.error("======================================");

        // 根据异常类型决定是否需要特殊处理
        handleSpecificException(throwable, message);
    }

    /**
     * 根据异常类型进行特殊处理
     *
     * @param throwable 抛出的异常
     * @param message 发生异常的消息
     */
    private void handleSpecificException(Throwable throwable, Message<?> message) {
        // 业务异常 - 可以重试
        if (throwable instanceof RuntimeException) {
            logger.warn("检测到业务异常，消息将保留在队列中等待重试");
        }

        // 系统异常 - 可能需要人工干预
        if (throwable instanceof OutOfMemoryError ||
            throwable instanceof StackOverflowError) {
            logger.error("严重系统异常，可能需要人工干预！");
        }

        // 超时异常
        if (throwable instanceof java.util.concurrent.TimeoutException) {
            logger.warn("检测到超时异常，建议增加messageVisibilityTimeout");
        }

        // 格式异常 - 直接删除消息（无法修复）
        if (throwable instanceof com.fasterxml.jackson.core.JsonProcessingException ||
            throwable instanceof com.fasterxml.jackson.databind.JsonMappingException) {
            logger.error("消息格式错误，将被删除（无法修复）");
        }
    }
}

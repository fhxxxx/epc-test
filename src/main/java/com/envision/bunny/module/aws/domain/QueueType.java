package com.envision.bunny.module.aws.domain;

import lombok.Getter;

/**
 * AWS SQS队列类型枚举
 * <p>
 * 定义所有可用的SQS队列类型,用于发送消息时指定目标队列
 *
 * @author example
 * @version 1.0.0
 */
@Getter
public enum QueueType {

    /**
     * 基础队列
     */
    BASIC_QUEUE("basicQueue", "基础队列"),

    /**
     * Header队列
     */
    HEADER_QUEUE("headerQueue", "Header队列"),

    /**
     * Headers队列
     */
    HEADERS_QUEUE("headersQueue", "Headers队列"),

    /**
     * 完整配置队列
     */
    FULL_CONFIG_QUEUE("fullConfigQueue", "完整配置队列"),

    /**
     * 批量队列
     */
    BATCH_QUEUE("batchQueue", "批量队列"),

    /**
     * 包装队列
     */
    WRAPPED_QUEUE("wrappedQueue", "包装队列"),

    /**
     * JSON队列
     */
    JSON_QUEUE("jsonQueue", "JSON队列"),

    /**
     * 广播队列1
     */
    BROADCAST_ONE_QUEUE("broadcastOneQueue", "广播队列1"),

    /**
     * 广播队列2
     */
    BROADCAST_TWO_QUEUE("broadcastTwoQueue", "广播队列2"),

    /**
     * 广播队列3
     */
    BROADCAST_THREE_QUEUE("broadcastThreeQueue", "广播队列3"),

    /**
     * 重试队列
     */
    RETRY_QUEUE("retryQueue", "重试队列"),

    /**
     * 死信队列
     */
    RETRY_QUEUE_DLQ("retryQueueDlq", "死信队列"),

    /**
     * 网关队列
     */
    GATEWAY_QUEUE("gatewayQueue", "网关队列");

    /**
     * 字段名称,对应AwsProperties中的字段名
     */
    private final String fieldName;

    /**
     * 队列描述
     */
    private final String description;

    QueueType(String fieldName, String description) {
        this.fieldName = fieldName;
        this.description = description;
    }

}

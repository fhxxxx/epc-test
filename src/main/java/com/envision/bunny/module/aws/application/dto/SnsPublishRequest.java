package com.envision.bunny.module.aws.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * SNS发布带属性消息请求对象
 *
 * 该类用于接收发布SNS消息时的请求参数
 * 包含消息主题、消息内容和自定义属性
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SnsPublishRequest {

    /**
     * 消息主题
     */
    private String subject;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 消息属性（键值对）
     */
    private Map<String, Object> attributes;
}


package com.envision.bunny.module.aws.application.dto;

import com.envision.bunny.module.aws.domain.QueueType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送JSON格式消息请求对象
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendJsonMessageRequest {

    /**
     * 队列类型枚举
     */
    private QueueType queueType;

    /**
     * 消息ID
     */
    private String id;

    /**
     * 用户名称
     */
    private String name;

    /**
     * 消息内容
     */
    private String content;
}

package com.envision.bunny.module.aws.application.dto;

import com.envision.bunny.module.aws.domain.QueueType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发送带属性消息请求对象
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageWithAttributesRequest {

    /**
     * 队列类型枚举
     */
    private QueueType queueType;

    /**
     * 消息内容
     */
    private String message;

    /**
     * 自定义发送者ID
     */
    private String senderId;
}

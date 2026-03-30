package com.envision.bunny.module.aws.application.dto;

import com.envision.bunny.module.aws.domain.QueueType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量发送消息请求对象
 *
 * @author example
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendBatchMessagesRequest {

    /**
     * 队列类型枚举
     */
    private QueueType queueType;

    /**
     * 要发送的消息数量
     */
    private int count;
}

package com.envision.bunny.module.aws.web;

import com.envision.bunny.module.aws.application.SqsService;
import com.envision.bunny.module.aws.application.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SQS消息测试控制器
 * <p>
 * 负责AWS SQS消息发送测试接口
 */
@RestController
@RequestMapping("/api/test")
public class SqsController {

    /**
     * 注入AWS注解方式消息服务
     * 该服务使用注解方式监听和处理SQS和SNS消息
     */
    @Autowired(required = false)
    private SqsService sqsService;

    // ==================== 注解方式测试接口 ====================

    /**
     * 发送消息到指定队列
     * <p>
     * 该接口演示基础的@SqsListener注解功能
     * 消息发送后会被对应监听器自动接收
     * <p>
     * 使用示例：
     * POST /api/test/annotation/sqs/send
     * {
     *   "queueType": "BASIC_QUEUE",
     *   "message": "HelloAnnotation"
     * }
     *
     * @param request 发送消息请求对象
     * @return 发送结果描述信息
     */
    @PostMapping("/annotation/sqs/send")
    public String sendMessageToQueue(@RequestBody SendMessageRequest request) {
        return sqsService.sendMessageToQueue(request.getQueueType(), request.getMessage());
    }



    /**
     * 发送带属性的消息到指定队列（测试@Header注解）
     * <p>
     * 该接口演示如何发送带有自定义属性的消息，
     * 这些属性可以被@Header注解提取
     * <p>
     * 使用示例：
     * POST /api/test/annotation/sqs/send-with-attributes
     * {
     *   "queueType": "HEADER_QUEUE",
     *   "message": "TestMessage",
     *   "senderId": "user123"
     * }
     *
     * @param request 发送带属性消息请求对象
     * @return 发送结果描述信息
     */
    @PostMapping("/annotation/sqs/send-with-attributes")
    public String sendMessageWithAttributes(@RequestBody SendMessageWithAttributesRequest request) {
        return sqsService.sendMessageWithAttributes(request.getQueueType(), request.getMessage(), request.getSenderId());
    }

    /**
     * 批量发送消息到指定队列（测试批量监听器）
     * <p>
     * 该接口演示如何批量发送多条消息，
     * 这些消息会被batchListener监听器批量接收
     * <p>
     * 使用示例：
     * POST /api/test/annotation/sqs/batch-send
     * {
     *   "queueType": "BATCH_QUEUE",
     *   "count": 5
     * }
     *
     * @param request 批量发送消息请求对象
     * @return 发送结果描述信息
     */
    @PostMapping("/annotation/sqs/batch-send")
    public String sendBatchMessages(@RequestBody SendBatchMessagesRequest request) {
        return sqsService.sendBatchMessages(request.getQueueType(), request.getCount());
    }

    /**
     * 发送JSON格式消息到指定队列（测试JSON对象自动反序列化）
     * <p>
     * 该接口演示如何发送JSON格式的消息，
     * 以及Spring Cloud AWS如何自动将其反序列化为Java对象
     * <p>
     * 使用示例：
     * POST /api/test/annotation/sqs/send-json
     * {
     *   "queueType": "JSON_QUEUE",
     *   "id": "msg001",
     *   "name": "User1",
     *   "content": "HelloJSON"
     * }
     *
     * @param request 发送JSON消息请求对象
     * @return 发送结果描述信息
     */
    @PostMapping("/annotation/sqs/send-json")
    public String sendJsonMessageToQueue(@RequestBody SendJsonMessageRequest request) {
        return sqsService.sendJsonMessageToQueue(request.getQueueType(), request.getId(), request.getName(), request.getContent());
    }

    /**
     * 使用流式API发送延迟消息到指定队列
     * <p>
     * 该接口演示如何使用SqsTemplate的流式API(SqsSendOptions)发送带有延迟投递的消息
     * <p>
     * 技术说明：
     * - 使用SqsTemplate.send(Consumer<SqsSendOptions>)流式API
     * - 延迟时间范围为0-900秒(15分钟)
     * - 这是SQS原生支持的延迟功能,通过delaySeconds()直接配置
     * - 相比使用Header的方式,流式API更直观、更符合Spring Cloud AWS最佳实践
     * <p>
     * 使用示例：
     * POST /api/test/annotation/sqs/send-delayed
     * {
     *   "queueType": "BASIC_QUEUE",
     *   "message": "延迟5秒的消息",
     *   "delaySeconds": 5
     * }
     *
     * @param request 发送延迟消息请求对象
     * @return 发送结果描述信息
     */
    @PostMapping("/annotation/sqs/send-delayed")
    public String sendDelayedMessage(@RequestBody SendDelayedMessageRequest request) {
        return sqsService.sendDelayedMessage(request.getQueueType(), request.getMessage(), request.getDelaySeconds());
    }
}

package com.envision.bunny.module.aws.application;

import com.envision.bunny.module.aws.application.dto.UserMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SQS消息监听器
 * <p>
 * 该类负责监听所有SQS队列的消息
 * 包括：基础消息、SNS消息、带属性的消息、批量消息、JSON消息等
 *
 * @author example
 * @version 1.0.0
 */
@Component
public class SqsMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(SqsMessageListener.class);

    /**
     * 注入消息存储服务，用于保存接收到的消息
     */
    @Autowired
    private MessageStorageService messageStorageService;

    /**
     * 监听器: 完整配置的监听器
     * <p>
     * 演示@SqsListener的配置选项：
     * - maxMessagesPerPoll: 每次拉取的最大消息数
     *
     * @param message 消息内容
     */
    /**
     * 高并发订单处理监听器
     * <p>
     * 场景：电商订单高峰期处理
     * 特点：
     * - 高并发（10个线程）
     * - 快速处理
     * - 自动ACK（ON_SUCCESS）
     * - 适用于大量小任务
     *
     * @SqsListener配置说明： - value: 监听的队列名称（支持占位符）
     * - factory: 使用highConcurrencyFactory配置
     * - maxMessagesPerPoll: 每次拉取的最大消息数
     */
    @SqsListener(
            queueNames = "${aws.annotation.sqs.full-config-queue.name}",//指定要监听的 SQS Queue 名称
            id = "fullConfigListener",                                    //Listener Container 的唯一标识
            factory = "highConcurrencyFactory",                     // 指定使用哪个 SqsMessageListenerContainerFactory
            maxMessagesPerPoll = "10",                               //单个线程 一次拉取 最多拉取的消息数
            pollTimeoutSeconds = "20",                               //SQS 长轮询时间
            acknowledgementMode = "ON_SUCCESS",                     /** 确认策略：ON_SUCCESS	方法成功 → 删除消息
                                                                        ALWAYS	方法结束就删
                                                                        MANUAL	手动 ACK
                                                                        */
            messageVisibilitySeconds = "60"                            //单条消息被拉取后,多久内对其他消费者不可见。必须 ≥ 最大可能处理时间，否则会导致重复消费
    )
    public void listenWithFullConfig(String message) {
        logger.info("[完整配置监听器] 接收到消息: {}", message);
        messageStorageService.saveMessage("fullConfigListener", message);
    }

    /**
     * 监听器: 基础 SQS 监听器
     * <p>
     * 使用@SqsListener注解自动监听指定的队列
     * 这是最简单的消息监听方式，队列中的消息会自动传递给此方法
     * <p>
     * @SqsListener属性说明:
     * - queueNames: 要监听的队列名称或URL数组
     * - id: 监听器的唯一标识符
     *
     * @param message 接收到的消息内容
     */
    @SqsListener(queueNames = "${aws.annotation.sqs.basic-queue.name}", id = "basicListener")
    public void listenToSqs(String message) {
        logger.info("[基础监听器] 接收到消息: {}", message);
        messageStorageService.saveMessage("basicListener", message);
    }

    /**
     * 监听器: 广播队列1监听器
     * <p>
     * 监听广播队列1的消息
     *
     * @param message 接收到的消息内容
     */
    @SqsListener(queueNames = "${aws.annotation.sqs.broadcast-one-queue.name}", id = "broadcastOneListener")
    public void listenToBroadcastOne(String message) {
        logger.info("[广播队列1监听器] 接收到消息: {}", message);
        messageStorageService.saveMessage("broadcastOneListener", message);
    }

    /**
     * 监听器: 广播队列2监听器
     * <p>
     * 监听广播队列2的消息
     *
     * @param message 接收到的消息内容
     */
    @SqsListener(queueNames = "${aws.annotation.sqs.broadcast-two-queue.name}", id = "broadcastTwoListener")
    public void listenToBroadcastTwo(String message) {
        logger.info("[广播队列2监听器] 接收到消息: {}", message);
        messageStorageService.saveMessage("broadcastTwoListener", message);
    }

    /**
     * 监听器: 广播队列3监听器
     * <p>
     * 监听广播队列3的消息
     *
     * @param message 接收到的消息内容
     */
    @SqsListener(queueNames = "${aws.annotation.sqs.broadcast-three-queue.name}", id = "broadcastThreeListener")
    public void listenToBroadcastThree(String message) {
        logger.info("[广播队列3监听器] 接收到消息: {}", message);
        messageStorageService.saveMessage("broadcastThreeListener", message);
    }

    /**
     * 监听器: 提取单个消息头
     * <p>
     * 使用@Header注解可以提取指定的消息头属性
     * 常用的SQS消息头包括：
     * - SenderId: 发送者ID
     * - ApproximateReceiveCount: 消息接收次数
     * - ApproximateFirstReceiveTimestamp: 首次接收时间戳
     * - MessageId: 消息ID
     * <p>
     * 注意：只有SNS主题发布消息时，通常会在消息中添加SenderId头，
     * 所以要设置required = false，避免接收到没有此属性的消息时报错
     *
     * @param message  消息内容
     * @param senderId 发送者ID（通过@Header注解提取）
     */
    @SqsListener(queueNames = "${aws.annotation.sqs.header-queue.name}", id = "headerListener")
    public void listenWithHeader(String message,
                               @Header(value = "SenderId", required = false) String senderId) {
        logger.info("[单个Header监听器] 消息: {}, 发送者ID: {}", message, senderId);
        messageStorageService.saveMessage("headerListener", String.format("消息: %s, 发送者ID: %s", message, senderId));
    }

    /**
     * 监听器: 提取所有消息头
     * <p>
     * 使用@Headers注解可以获取所有消息头，以Map形式返回
     * 这对于调试和查看完整的消息元数据非常有用
     * <p>
     * 常见的消息头包括:
     * <pre>
     * {
     *   "Sqs_QueueAttributes": "...",
     *   "Sqs_QueueUrl": "https://sqs.us-east-1.amazonaws.com/...",
     *   "Sqs_ReceiptHandle": "...",
     *   "Sqs_ReceivedAt": "...",
     *   "Sqs_Msa_ApproximateReceiveCount": "1",
     *   "Sqs_Msa_SentTimestamp": "...",
     *   "Sqs_Msa_ApproximateFirstReceiveTimestamp": "...",
     *   "SenderId": "121212",
     *   "Sqs_QueueName": "...",
     *   "Sqs_Msa_SenderId": "...",
     *   "id": "...",
     *   "Sqs_VisibilityTimeout": "...",
     *   "timestamp": "..."
     * }
     * </pre>
     *
     * @param message 消息内容
     * @param headers 所有消息头（Map形式）
     */
    @SqsListener(queueNames = "${aws.annotation.sqs.headers-queue.name}", id = "headersListener")
    public void listenWithHeaders(String message,
                                 @Headers Map<String, Object> headers) {
        logger.info("[所有Header监听器] 消息: {}, 消息头数量: {}", message, headers.size());
        // 打印所有消息头
        headers.forEach((key, value) -> logger.debug("  {}: {}", key, value));
        messageStorageService.saveMessage("headersListener", String.format("消息: %s, 消息头: %s", message, headers));
    }

    /**
     * 监听器: 批量消息监听器
     * <p>
     * 使用List<String>参数可以一次接收多条消息
     * 结合maxMessagesPerPoll属性，可以实现高效的消息批量处理
     *
     * @param messages 消息列表
     */
    @SqsListener(
        queueNames = "${aws.annotation.sqs.batch-queue.name}",
        id = "batchListener",
        maxMessagesPerPoll = "10"
    )
    public void listenToBatch(List<String> messages) {
        logger.info("[批量监听器] 批量接收到 {} 条消息", messages.size());
        messages.forEach(msg -> logger.debug("  - {}", msg));
        messageStorageService.saveMessage("batchListener", String.format("批量接收 %d 条消息: %s", messages.size(), messages));
    }

    /**
     * 监听器: 包装类型消息监听器
     * <p>
     * 使用Spring Message对象可以访问消息的完整信息：
     * - 消息头（Headers）
     * - 消息体（Payload）
     * - 消息属性（Properties）
     *
     * @param message Spring Message包装对象
     */
    @SqsListener(queueNames = "${aws.annotation.sqs.wrapped-queue.name}", id = "wrappedListener")
    public void listenToWrappedMessage(Message<String> message) {
        logger.info("[包装消息监听器] 消息体: {}, 消息头: {}",
                    message.getPayload(), message.getHeaders());
        messageStorageService.saveMessage("wrappedListener", String.format("消息: %s, 头: %s",
                message.getPayload(), message.getHeaders()));
    }

    /**
     * 监听器: JSON对象消息监听器
     * <p>
     * Spring Cloud AWS会自动将JSON格式的消息反序列化为Java对象
     *
     * @param userMessage 自动反序列化的UserMessage对象
     */
    @SqsListener(queueNames = "${aws.annotation.sqs.json-queue.name}", id = "jsonListener")
    public void listenToJsonMessage(UserMessage userMessage) {
        logger.info("[JSON对象监听器] 接收到用户消息: {}", userMessage);
        messageStorageService.saveMessage("jsonListener", userMessage.toString());
    }

    /**
     * 监听器: 重试队列监听器
     * <p>
     * 演示重试机制的消息监听，用于处理需要重试的任务
     * <p>
     * 配置说明:
     * - acknowledgementMode = "ON_SUCCESS": 仅在方法执行成功时删除消息，抛出异常时保留消息
     * - messageVisibilitySeconds = "5": 演示场景，5秒后消息重新可见，便于快速观察重试效果
     * <p>
     * 重试逻辑:
     * - 从消息头中获取 ApproximateReceiveCount 记录重试次数
     * - 抛出异常触发重试，直到消息达到队列配置的最大接收次数后被移到死信队列
     * <p>
     * 说明:
     * - 使用 Message<String> 包装对象以便访问完整的消息头
     * - ApproximateReceiveCount 是 SQS 的系统属性，记录消息被接收的次数
     *
     * @param message Spring Message包装对象
     */
    @SqsListener(queueNames = "${aws.annotation.sqs.retry-queue.name}", id = "retryQueueListener",
                  acknowledgementMode = "ON_SUCCESS", messageVisibilitySeconds = "5")
    public void listenToRetryQueue(Message<String> message) {
        String payload = message.getPayload();

        // 从消息头中获取 ApproximateReceiveCount
        String receiveCount = message.getHeaders().get("Sqs_Msa_ApproximateReceiveCount", String.class);

        logger.info("[重试队列监听器] 接收到消息: {} (第{}次尝试)", payload, receiveCount);

        try {
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            messageStorageService.saveMessage("retryQueueListener",
                    String.format("[%s] 消息: %s (第%s次尝试)", timestamp, payload, receiveCount));
        } catch (Exception e) {
            logger.error("[重试队列监听器] 保存消息失败: {}", e.getMessage());
        }

        throw new RuntimeException(String.format("消息处理失败(第 %s 次尝试): %s",
                receiveCount, payload));
    }

    /**
     * 监听器: 死信队列监听器
     * <p>
     * 监听死信队列的消息，用于处理无法正常处理的消息
     *
     * @param message 接收到的消息内容
     */
    @SqsListener(queueNames = "${aws.annotation.sqs.retry-queue-dlq.name}", id = "retryQueueDlqListener")
    public void listenToRetryQueueDlq(String message) {
        logger.info("[死信队列监听器] 接收到消息: {}", message);
        messageStorageService.saveMessage("retryQueueDlqListener", message);
    }
}

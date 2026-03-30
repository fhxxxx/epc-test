package com.envision.bunny.module.aws.application;

import com.envision.bunny.infrastructure.aws.AwsProperties;
import com.envision.bunny.module.aws.application.dto.UserMessage;
import com.envision.bunny.module.aws.domain.QueueType;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AWS消息发送服务类
 * <p>
 * 该类负责发送消息到AWS SQS队列
 * 支持发送基础消息、带属性的消息、批量消息、JSON消息等
 *
 * @author example
 * @version 1.0.0
 */
@Service
public class SqsService {

    private static final Logger logger = LoggerFactory.getLogger(SqsService.class);
    /**
     * SQS批量发送最大消息数限制
     */
    private static final int SQS_BATCH_MAX_MESSAGES = 10;

    /**
     * SQS模板，用于发送消息到队列
     */
    @Autowired(required = false)
    private SqsTemplate sqsTemplate;

    /**
     * AWS配置属性
     */
    @Autowired
    private AwsProperties awsProperties;

    /**
     * 发送消息到指定队列
     * <p>
     * 使用SqsTemplate发送消息到指定的队列
     *
     * @param queueType 队列类型枚举
     * @param message  要发送的消息内容
     * @return 发送结果
     */
    public String sendMessageToQueue(QueueType queueType, String message) {
        String queueUrl = getQueueUrl(queueType);
        logger.info("发送消息到队列[{}]: {}", queueType.getDescription(), queueUrl);
        sqsTemplate.send(queueUrl, message);
        logger.info("消息发送成功: {}", message);
        return String.format("消息已发送到%s: %s", queueType.getDescription(), message);
    }

    /**
     * 发送带属性的消息到指定队列
     * <p>
     * 演示如何发送带有消息属性的消息
     * 这些属性可以被@Header注解提取
     *
     * @param queueType 队列类型枚举
     * @param message   消息内容
     * @param senderId  自定义发送者ID
     * @return 发送结果
     */
    public String sendMessageWithAttributes(QueueType queueType, String message, String senderId) {
        String queueUrl = getQueueUrl(queueType);
        logger.info("发送带属性的消息到队列[{}]: {}, 发送者ID: {}", queueType.getDescription(), queueUrl, senderId);
        Message<String> springMessage = MessageBuilder.withPayload(message)
                .setHeader("SenderId", senderId)
                .build();
        sqsTemplate.send(queueUrl, springMessage);
        return String.format("带属性的消息已发送到%s: %s", queueType.getDescription(), message);
    }

    /**
     * 批量发送消息到指定队列
     * <p>
     * 使用SqsTemplate的sendMany方法批量发送多条消息
     * <p>
     * 性能说明:
     * - SQS SendMessageBatch API一次最多发送10条消息
     * - 相比循环调用send(),批量发送可显著减少API调用次数和网络延迟
     * - 自动处理超过10条消息的情况,分批发送
     *
     * @param queueType 队列类型枚举
     * @param count     要发送的消息数量
     * @return 发送结果
     */
    public String sendBatchMessages(QueueType queueType, int count) {
        String queueUrl = getQueueUrl(queueType);
        logger.info("批量发送 {} 条消息到队列[{}]: {}", count, queueType.getDescription(), queueUrl);

        int sentCount = 0;
        int batchSize;
        String timestamp = LocalDateTime.now().toString();

        // 分批发送,每批最多10条
        while (sentCount < count) {
            batchSize = Math.min(count - sentCount, SQS_BATCH_MAX_MESSAGES);

            List<Message<String>> batchMessages = new ArrayList<>();
            for (int i = 0; i < batchSize; i++) {
                int messageNum = sentCount + i + 1;
                String messageContent = String.format("批量消息 %d/%d - %s", messageNum, count, timestamp);
                batchMessages.add(MessageBuilder.withPayload(messageContent).build());
            }

            // 使用批量发送API
            sqsTemplate.sendMany(queueUrl, batchMessages);

            sentCount += batchSize;
            logger.info("已发送第 {} 批消息,共 {} 条", (sentCount / SQS_BATCH_MAX_MESSAGES) + 1, sentCount);
        }

        logger.info("批量发送 {} 条消息成功", count);
        return String.format("已批量发送 %d 条消息到%s", count, queueType.getDescription());
    }

    /**
     * 发送JSON格式消息到指定队列
     * <p>
     * 发送JSON格式的消息，测试JSON对象自动反序列化功能
     *
     * @param queueType 队列类型枚举
     * @param id        消息ID
     * @param name      用户名称
     * @param content   消息内容
     * @return 发送结果
     */
    public String sendJsonMessageToQueue(QueueType queueType, String id, String name, String content) {
        UserMessage userMessage = new UserMessage(
                id,
                name,
                content,
                LocalDateTime.now()
        );

        String queueUrl = getQueueUrl(queueType);
        logger.info("发送JSON格式消息到队列[{}]: {}", queueType.getDescription(), queueUrl);
        logger.info("JSON消息内容: {}", userMessage);

        // 将对象序列化为JSON字符串并发送
        String jsonMessage = String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"content\":\"%s\",\"timestamp\":\"%s\"}",
                userMessage.getId(),
                userMessage.getName(),
                userMessage.getContent(),
                userMessage.getTimestamp()
        );

        sqsTemplate.send(queueUrl, jsonMessage);

        logger.info("JSON消息发送成功");
        return String.format("JSON消息已发送到%s: %s", queueType.getDescription(), userMessage);
    }

    /**
     * 使用流式API发送延迟消息到指定队列
     * <p>
     * 使用SqsSendOptions流式API发送带有延迟投递的消息
     * <p>
     * 技术说明:
     * - 使用SqsTemplate.send(Consumer<SqsSendOptions>)流式API
     * - 延迟时间范围为0-900秒(15分钟)
     * - 这是SQS原生支持的延迟功能,通过delaySeconds()直接配置
     * - 相比使用Header的方式,流式API更直观、更符合Spring Cloud AWS最佳实践
     *
     * @param queueType 队列类型枚举
     * @param message   消息内容
     * @param delaySeconds 延迟时间(秒),范围0-900
     * @return 发送结果
     */
    public String sendDelayedMessage(QueueType queueType, String message, int delaySeconds) {
        String queueUrl = getQueueUrl(queueType);
        logger.info("使用流式API发送延迟消息到队列[{}]: {}, 延迟{}秒", queueType.getDescription(), queueUrl, delaySeconds);

        // 使用SqsSendOptions流式API原生支持的delaySeconds方法
        sqsTemplate.send(options -> options
                .queue(queueUrl)
                .payload(message)
                .delaySeconds(delaySeconds)
        );

        logger.info("延迟消息发送成功,将{}秒后投递", delaySeconds);
        return String.format("延迟消息已发送到%s,将在%d秒后投递: %s", queueType.getDescription(), delaySeconds, message);
    }

    /**
     * 根据队列类型枚举获取队列URL
     *
     * @param queueType 队列类型枚举
     * @return 队列URL
     */
    private String getQueueUrl(QueueType queueType) {
        switch (queueType) {
            case BASIC_QUEUE:
                return awsProperties.getSqs().getBasicQueue().getUrl();
            case HEADER_QUEUE:
                return awsProperties.getSqs().getHeaderQueue().getUrl();
            case HEADERS_QUEUE:
                return awsProperties.getSqs().getHeadersQueue().getUrl();
            case FULL_CONFIG_QUEUE:
                return awsProperties.getSqs().getFullConfigQueue().getUrl();
            case BATCH_QUEUE:
                return awsProperties.getSqs().getBatchQueue().getUrl();
            case WRAPPED_QUEUE:
                return awsProperties.getSqs().getWrappedQueue().getUrl();
            case JSON_QUEUE:
                return awsProperties.getSqs().getJsonQueue().getUrl();
            case BROADCAST_ONE_QUEUE:
                return awsProperties.getSqs().getBroadcastOneQueue().getUrl();
            case BROADCAST_TWO_QUEUE:
                return awsProperties.getSqs().getBroadcastTwoQueue().getUrl();
            case BROADCAST_THREE_QUEUE:
                return awsProperties.getSqs().getBroadcastThreeQueue().getUrl();
            case RETRY_QUEUE:
                return awsProperties.getSqs().getRetryQueue().getUrl();
            case RETRY_QUEUE_DLQ:
                return awsProperties.getSqs().getRetryQueueDlq().getUrl();
            default:
                throw new IllegalArgumentException("不支持的队列类型: " + queueType);
        }
    }
}

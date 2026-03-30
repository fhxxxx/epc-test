package com.envision.bunny.module.aws.application;

import com.envision.bunny.infrastructure.aws.AwsProperties;
import io.awspring.cloud.sns.core.SnsNotification;
import io.awspring.cloud.sns.core.SnsTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;

import java.util.Map;

/**
 * AWS SNS服务测试类
 */
@Slf4j
@Service
public class SnsService {

    @Autowired(required = false)
    private SnsTemplate snsTemplate;

    /**
     * AWS SNS客户端
     */
    @Autowired(required = false)
    private SnsClient snsClient;

    /**
     * AWS配置属性
     */
    @Autowired
    private AwsProperties awsProperties;

    /**
     * 通过SNS发布消息到SQS
     * <p>
     * 当SQS队列订阅了SNS主题时，通过SNS发布的消息会自动传递到SQS队列
     *
     * @param message 消息内容
     * @param subject 消息主题
     * @return 发布结果
     */
    public String publishSnsToSqs(String message, String subject) {
        log.info("通过SNS发布消息到SQS（使用SnsTemplate） - 主题: {}, 消息: {}", subject, message);

        // 使用 SnsTemplate 的 sendNotification 方法，简洁地发送消息
        snsTemplate.sendNotification(awsProperties.getSns().getTopic().getArn(), message, subject);

        log.info("SNS消息发布成功，等待推送到SQS队列");
        return String.format("SNS消息已发布 - 主题: %s, 消息: %s", subject, message);
    }

    /**
     * 通过SNS发布带属性的消息到SQS
     * <p>
     * 该方法在原有publishSnsToSqs方法基础上，增加了对消息属性的支持
     * 消息属性可以在接收端通过@Header注解提取
     * <p>
     * 示例：
     * <pre>
     * Map&lt;String, String&gt; attributes = new HashMap&lt;&gt;();
     * attributes.put("senderId", "user123");
     * attributes.put("messageType", "test");
     * attributes.put("priority", "1");
     * publishSnsWithAttributes("Hello SNS", "Test Subject", attributes);
     * </pre>
     *
     * @param message         消息内容
     * @param subject        消息主题
     * @param messageAttributes 消息属性Map(String, String)
     * @return 发布结果
     */
    public String publishSnsWithAttributes(String message, String subject, Map<String, Object> messageAttributes) {
        log.info("通过SNS发布带属性的消息到SQS - 主题: {}, 消息: {}, 属性数量: {}",
                subject, message, messageAttributes.size());

//        // 将String类型的属性值转换为SNS MessageAttributeValue
//        Map<String, MessageAttributeValue> snsAttributes = new HashMap<>();
//        messageAttributes.forEach((key, value) -> {
//            MessageAttributeValue attributeValue = MessageAttributeValue.builder()
//                    .stringValue(value)
//                    .dataType("String")
//                    .build();
//            snsAttributes.put(key, attributeValue);
//            log.debug("添加消息属性 - 键: {}, 值: {}, 类型: {}", key, value, "String");
//        });
//
//        // 构建PublishRequest并设置消息属性
//        PublishRequest publishRequest = PublishRequest.builder()
//                .topicArn(awsProperties.getSns().getTopic().getArn())
//                .message(message)
//                .subject(subject)
//                .messageAttributes(snsAttributes)
//                .build();
//
//        // 使用SnsClient发送消息
//        snsClient.publish(publishRequest);

        SnsNotification<String> notification =
                SnsNotification.builder(message)
                        .subject(subject)
                        .headers(messageAttributes)
                        .build();

        snsTemplate.sendNotification(awsProperties.getSns().getTopic().getArn(), notification);

        log.info("带属性的SNS消息发布成功 - 主题: {}, 消息: {}, 属性数量: {}",
                subject, message, messageAttributes.size());
        return String.format("带属性的SNS消息已发布 - 主题: %s, 消息: %s, 属性数量: %d",
                subject, message, messageAttributes.size());
    }
}

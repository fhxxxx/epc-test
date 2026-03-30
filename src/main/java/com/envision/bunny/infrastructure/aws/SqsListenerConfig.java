package com.envision.bunny.infrastructure.aws;

import com.envision.bunny.module.aws.application.CustomErrorHandler;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.Duration;

/**
 * SQS监听器配置类
 * <p>
 * 该类配置了SqsMessageListenerContainerFactory，用于高并发生产环境场景
 *
 * @author example
 * @version 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "spring.cloud.aws.sqs.enabled", havingValue = "true", matchIfMissing = true)
public class SqsListenerConfig {

    private static final Logger logger = LoggerFactory.getLogger(SqsListenerConfig.class);

    /**
     * SQS异步客户端
     */
    @Autowired(required = false)
    private SqsAsyncClient sqsAsyncClient;

    /**
     * 高并发场景工厂
     * <p>
     * 适用场景：
     * - 电商订单高峰期处理
     * - 高吞吐量场景
     * - 快速处理大量小任务
     * <p>
     * 配置特点：
     * - 高并发线程数（10）
     * - 较大的批量处理（10条/次）
     * - 适中的可见性超时（60秒）
     * - 长轮询时间（20秒）
     */
    @Bean(name = "highConcurrencyFactory")
    public SqsMessageListenerContainerFactory<Object> highConcurrencyFactory() {
        logger.info("初始化高并发监听器工厂...");

        SqsMessageListenerContainerFactory<Object> factory = new SqsMessageListenerContainerFactory<>();
        factory.setSqsAsyncClient(sqsAsyncClient);
        factory.configure(options -> options
                .maxConcurrentMessages(10)      // 最大并发处理消息数
                .maxMessagesPerPoll(10)          // 每次轮询获取的最大消息数
                .messageVisibility(Duration.ofSeconds(60))  // 消息可见性超时
                .pollTimeout(Duration.ofSeconds(20))  // 轮询超时时间
                .acknowledgementMode(AcknowledgementMode.ON_SUCCESS)
        );
        factory.setErrorHandler(new CustomErrorHandler());
        logger.info("高并发监听器工厂配置完成 - 并发: 10, 最大消息数/次: 10, 可见性超时: 60秒, 轮询超时: 20秒");
        return factory;
    }

    @Bean
    public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(SqsAsyncClient sqsAsyncClient) {

        return SqsMessageListenerContainerFactory.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .build();
    }
}

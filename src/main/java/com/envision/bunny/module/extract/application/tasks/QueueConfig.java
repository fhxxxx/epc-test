package com.envision.bunny.module.extract.application.tasks;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * @author wenjun.gu
 * @since 2025/8/19-11:18
 */
@Configuration
public class QueueConfig {
    @Bean
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> ocrTaskContainer(
            RedisConnectionFactory connectionFactory, OcrTaskConsumer ocrTaskConsumer) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(200))
                        .targetType(String.class)
                        .executor(Executors.newSingleThreadExecutor())
                        .batchSize(1)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, String>> listenerContainer =
                StreamMessageListenerContainer.create(connectionFactory, options);

        listenerContainer.receive(
                Consumer.from(Constants.EXTRACT_GROUP, Constants.EXTRACT_CONSUMER),
                StreamOffset.create(Constants.OCR_TASK, ReadOffset.lastConsumed()), ocrTaskConsumer);

        listenerContainer.start();

        return listenerContainer;
    }

    @Bean
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> ocrListeningTaskContainer(
            RedisConnectionFactory connectionFactory, OcrListeningTaskConsumer ocrListeningTaskConsumer) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(200))
                        .targetType(String.class)
                        .executor(Executors.newSingleThreadExecutor())
                        .batchSize(1)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, String>> listenerContainer =
                StreamMessageListenerContainer.create(connectionFactory, options);

        listenerContainer.receive(
                Consumer.from(Constants.EXTRACT_GROUP, Constants.EXTRACT_CONSUMER),
                StreamOffset.create(Constants.OCR_LISTENING_TASK, ReadOffset.lastConsumed()), ocrListeningTaskConsumer);

        listenerContainer.start();

        return listenerContainer;
    }

    @Bean
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> extractTaskContainer(
            RedisConnectionFactory connectionFactory, ExtractTaskConsumer extractTaskConsumer) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(200))
                        .targetType(String.class)
                        .executor(Executors.newSingleThreadExecutor())
                        .batchSize(1)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, String>> listenerContainer =
                StreamMessageListenerContainer.create(connectionFactory, options);

        listenerContainer.receive(
                Consumer.from(Constants.EXTRACT_GROUP, Constants.EXTRACT_CONSUMER),
                StreamOffset.create(Constants.EXTRACT_TASK, ReadOffset.lastConsumed()), extractTaskConsumer);

        listenerContainer.start();

        return listenerContainer;
    }

    @Bean
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> compareTaskContainer(
            RedisConnectionFactory connectionFactory, CompareTaskConsumer compareTaskConsumer) {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        .pollTimeout(Duration.ofMillis(200))
                        .targetType(String.class)
                        .executor(Executors.newSingleThreadExecutor())
                        .batchSize(1)
                        .build();

        StreamMessageListenerContainer<String, ObjectRecord<String, String>> listenerContainer =
                StreamMessageListenerContainer.create(connectionFactory, options);

        listenerContainer.receive(
                Consumer.from(Constants.EXTRACT_GROUP, Constants.EXTRACT_CONSUMER),
                StreamOffset.create(Constants.COMPARE_TASK, ReadOffset.lastConsumed()), compareTaskConsumer);

        listenerContainer.start();

        return listenerContainer;
    }

}

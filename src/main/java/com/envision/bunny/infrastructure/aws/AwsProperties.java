package com.envision.bunny.infrastructure.aws;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AWS注解方式配置属性类
 *
 * 使用@ConfigurationProperties注解绑定前缀为"aws.annotation"的配置项
 * 提供对SQS和SNS相关配置的类型安全访问
 *
 * @author example
 * @version 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "aws.annotation")
public class AwsProperties {

    /**
     * SQS相关配置
     */
    private SqsProperties sqs = new SqsProperties();

    /**
     * SNS相关配置
     */
    private SnsProperties sns = new SnsProperties();

    /**
     * S3相关配置
     */
    private S3Properties s3 = new S3Properties();

    /**
     * SQS配置内部类
     */
    @Data
    public static class SqsProperties {
        /**
         * 基础队列配置
         */
        private QueueProperties basicQueue = new QueueProperties();

        /**
         * Header队列配置
         */
        private QueueProperties headerQueue = new QueueProperties();

        /**
         * Headers队列配置
         */
        private QueueProperties headersQueue = new QueueProperties();

        /**
         * 完整配置队列
         */
        private QueueProperties fullConfigQueue = new QueueProperties();

        /**
         * 批量队列配置
         */
        private QueueProperties batchQueue = new QueueProperties();

        /**
         * 包装队列配置
         */
        private QueueProperties wrappedQueue = new QueueProperties();

        /**
         * JSON队列配置
         */
        private QueueProperties jsonQueue = new QueueProperties();

        /**
         * 广播队列1配置
         */
        private QueueProperties broadcastOneQueue = new QueueProperties();

        /**
         * 广播队列2配置
         */
        private QueueProperties broadcastTwoQueue = new QueueProperties();

        /**
         * 广播队列3配置
         */
        private QueueProperties broadcastThreeQueue = new QueueProperties();

        /**
         * 重试队列配置
         */
        private QueueProperties retryQueue = new QueueProperties();

        /**
         * 死信队列配置
         */
        private QueueProperties retryQueueDlq = new QueueProperties();

        /**
         * 队列配置内部类
         */
        @Data
        public static class QueueProperties {
            /**
             * 队列名称
             */
            private String name = "";

            /**
             * 队列URL（完整URL）
             */
            private String url = "";
        }
    }

    /**
     * SNS配置内部类
     */
    @Data
    public static class SnsProperties {
        /**
         * 基础主题配置
         */
        private TopicProperties topic = new TopicProperties();

        /**
         * 主题配置内部类
         */
        @Data
        public static class TopicProperties {
            /**
             * 主题ARN（完整ARN）
             */
            private String arn = "";
        }
    }

    /**
     * S3配置内部类
     */
    @Data
    public static class S3Properties {
        /**
         * 基础存储桶配置
         */
        private BucketProperties basicBucket = new BucketProperties();

        /**
         * 存储桶配置内部类
         */
        @Data
        public static class BucketProperties {
            /**
             * 存储桶名称
             */
            private String name = "";
        }
    }
}

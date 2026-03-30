package com.envision.bunny.module.aws.web;

import com.envision.bunny.module.aws.application.SnsService;
import com.envision.bunny.module.aws.application.dto.SnsPublishRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SNS消息控制器
 * <p>
 * 负责AWS SNS消息发布相关接口
 */
@RestController
@RequestMapping("/api/sns")
public class SnsController {

    /**
     * 注入AWS SNS消息服务
     * 该服务负责SNS消息发布
     */
    @Autowired(required = false)
    private SnsService snsService;

    /**
     * 通过SNS发布消息（测试@NotificationMessage和@NotificationSubject）
     * <p>
     * 该接口演示SNS消息如何通过SQS队列传递，
     * 以及如何使用@NotificationMessage和@NotificationSubject注解提取消息
     * <p>
     * 注意：需要SQS队列已订阅SNS主题
     * <p>
     * 使用示例：
     * POST /api/sns/publish
     * {
     *   "subject": "TestSubject",
     *   "message": "TestMessage"
     * }
     *
     * @param request SNS发布请求对象
     * @return 发布结果描述信息
     */
    @PostMapping("/publish")
    public String publishSns(@RequestBody SnsPublishRequest request) {
        return snsService.publishSnsToSqs(request.getMessage(), request.getSubject());
    }

    /**
     * 通过SNS发布带属性的消息
     * <p>
     * 该接口演示如何发布带有自定义消息属性的SNS消息
     * 消息属性可以被接收方通过消息头获取
     * <p>
     * 注意：需要SQS队列已订阅SNS主题
     * <p>
     * 使用示例：
     * POST /api/sns/publish-with-attributes
     * Content-Type: application/json
     * {
     *   "subject": "TestSubject",
     *   "message": "TestMessage",
     *   "attributes": {
     *     "source": "system",
     *     "priority": "high"
     *   }
     * }
     *
     * @param request SNS发布请求对象
     * @return 发布结果描述信息
     */
    @PostMapping("/publish-with-attributes")
    public String publishSnsWithAttributes(@RequestBody SnsPublishRequest request) {
        return snsService.publishSnsWithAttributes(
                request.getMessage(),
                request.getSubject(),
                request.getAttributes()
        );
    }
}

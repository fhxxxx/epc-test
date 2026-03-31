package com.envision.bunny.module.extract.application.tasks;

import com.envision.extract.infrastructure.log.AvoidLog;
import com.envision.extract.module.extract.application.ExtractCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/19-19:59
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OcrListeningTaskConsumer implements StreamListener<String, ObjectRecord<String, String>> {
    private final RedisTemplate<String, String> redisTemplate;
    private final ExtractCommandService extractCommandService;

    @Value("${custom.extract.over-time-limit.ocr-listening-task}")
    private Long timeLimit;


    @Override
    public void onMessage(ObjectRecord<String, String> message) {
        String runId = message.getValue();
        extractCommandService.listenOcr(Long.valueOf(runId));
        redisTemplate.opsForStream().acknowledge(Constants.EXTRACT_GROUP, message);

        Long result = redisTemplate.opsForStream().delete(Constants.OCR_LISTENING_TASK, message.getId());
        log.info("redis-stream：[{}], 消息：[{}], 消费成功，删除结果：[{}]", Constants.OCR_LISTENING_TASK, message, result);
    }

    /**
     * 每分钟进行重试任务
     */
    @Scheduled(cron = "0 */10 * * * ?")
    @AvoidLog
    public void retryTask() {
        // 1. 获取 pending 消息
        PendingMessages pendingMessages = redisTemplate.opsForStream()
                .pending(Constants.OCR_LISTENING_TASK, Constants.EXTRACT_GROUP, Range.unbounded(), 100);

        // 拉取阻塞时间超过限定配置的消息进行处理, 10分钟
        List<PendingMessage> overTimeMessages = pendingMessages.stream()
                .filter(pendingMessage -> pendingMessage.getElapsedTimeSinceLastDelivery().toMillis() > timeLimit).toList();
        for (PendingMessage pm : overTimeMessages) {
            String messageId = pm.getIdAsString();
            log.info("redis-stream：[{}], 消息：[{}], 阻塞时间[{}], 阻塞消息开始处理", Constants.OCR_LISTENING_TASK, messageId,
                    pm.getElapsedTimeSinceLastDelivery().toMillis());

            List<MapRecord<String, Object, Object>> claimedMessages =
                    redisTemplate.opsForStream().claim(Constants.OCR_LISTENING_TASK, Constants.EXTRACT_GROUP, Constants.EXTRACT_CONSUMER,
                            Duration.ofSeconds(0), RecordId.of(messageId));

            for (MapRecord<String, Object, Object> record : claimedMessages) {
                //异常业务处理 不重试，直接更新状态
                extractCommandService.overTimeOcrListeningTask(Long.valueOf(String.valueOf(record.getValue().get(Constants.EXTRACT_RUN_KEY))));
                redisTemplate.opsForStream().acknowledge(Constants.OCR_LISTENING_TASK, Constants.EXTRACT_GROUP, record.getId());

                Long result = redisTemplate.opsForStream().delete(Constants.OCR_LISTENING_TASK, messageId);
                log.info("redis-stream：[{}], 消息：[{}], 阻塞异常处理成功，删除结果：[{}]", Constants.OCR_LISTENING_TASK, record.getId(), result);
            }
        }
    }
}

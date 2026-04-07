package com.envision.epc.module.extract.application.tasks;

import com.envision.epc.infrastructure.log.AvoidLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author wenjun.gu
 * @since 2025/8/19-11:39
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class MessageProducer {
    private final RedisTemplate<String, String> redisTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public void sendMessage(String streamKey, Map<String, String> message) {
        RecordId recordId = redisTemplate.opsForStream().add(streamKey, message);
        if (recordId != null) {
            log.info("Message sent to Stream '" + streamKey + "' with RecordId: " + recordId);
        }
    }

    @AvoidLog
    @Scheduled(fixedRate = 1000)
    public void moveReadyTasksToStream() {
        long now = System.currentTimeMillis() / 1000;

        Set<String> tasks = redisTemplate.opsForZSet().rangeByScore(Constants.OCR_DELAYED_TASK, 0, now);

        if (tasks != null && !tasks.isEmpty()) {
            for (String task:  tasks) {
                try {
                    sendMessage(Constants.OCR_LISTENING_TASK, objectMapper.readValue(task, Map.class));
                    redisTemplate.opsForZSet().remove(Constants.OCR_DELAYED_TASK, task);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @PostConstruct
    public void initializeStream() {
        // 必须确保三个 Stream 的消费组都创建成功，否则抛异常
        createStreamAndGroup(Constants.COMPARE_TASK, Constants.EXTRACT_GROUP, false);
    }

    private void createStreamAndGroup(String streamKey, String groupName, boolean fromStart) {
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            try {
                // MKSTREAM = true 表示如果 Stream 不存在则自动创建（Redis 6.2+）
                connection.streamCommands().xGroupCreate(
                        streamKey.getBytes(),
                        groupName,
                        fromStart ? ReadOffset.from("0") : ReadOffset.latest(),
                        true // MKSTREAM
                );
                log.info("消费组 [{}] 已在 Stream [{}] 创建成功", groupName, streamKey);
            } catch (RedisSystemException e) {
                if (isBusyGroupError(e)) {
                    // 消费组已存在
                    log.info("消费组 [{}] 在 Stream [{}] 已存在，跳过创建", groupName, streamKey);
                } else {
                    // 其它 Redis 错误，直接抛出
                    log.error("创建消费组 [{}] 在 Stream [{}] 失败", groupName, streamKey, e);
                    throw e;
                }
            } catch (DataAccessException e) {
                log.error("Redis 访问异常，创建消费组 [{}] 在 Stream [{}] 失败", groupName, streamKey, e);
                throw e;
            } catch (Exception e) {
                log.error("未知异常，创建消费组 [{}] 在 Stream [{}] 失败", groupName, streamKey, e);
                throw e;
            }
            return null;
        });
    }

    private boolean isBusyGroupError(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.contains("BUSYGROUP")) {
                return true;
            }
            if (current.getClass().getName().contains("RedisBusyException")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

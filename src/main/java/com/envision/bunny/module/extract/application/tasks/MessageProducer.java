package com.envision.bunny.module.extract.application.tasks;

import com.envision.extract.infrastructure.log.AvoidLog;
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

import javax.annotation.PostConstruct;
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

    public void sendMessages(String streamKey, List<Map<String, String>> messages) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringConn = (StringRedisConnection) connection;
            for (Map<String, String> msg : messages) {
                stringConn.xAdd(streamKey, msg);
            }
            return null;
        });
    }

    public void sendDelayMessage(Map<String, String> message, long delayInSeconds) {
        long score = System.currentTimeMillis() / 1000 + delayInSeconds;
        try {
            String s = objectMapper.writeValueAsString(message);
            redisTemplate.opsForZSet().add(Constants.OCR_DELAYED_TASK, s, score);
            log.info("Message sent to Stream '" + Constants.OCR_DELAYED_TASK + "' with RecordId: " + s);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
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
        createStreamAndGroup(Constants.OCR_TASK, Constants.EXTRACT_GROUP, false);
        createStreamAndGroup(Constants.OCR_LISTENING_TASK, Constants.EXTRACT_GROUP, false);
        createStreamAndGroup(Constants.EXTRACT_TASK, Constants.EXTRACT_GROUP, false);
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
                if (e.getMessage() != null && e.getMessage().contains("BUSYGROUP")) {
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
}

package com.envision.bunny.module.aws.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息存储服务
 * <p>
 * 负责存储和查询监听器接收到的消息
 * 使用ConcurrentHashMap保证线程安全
 *
 * @author example
 * @version 1.0.0
 */
@Service
public class MessageStorageService {

    private static final Logger logger = LoggerFactory.getLogger(MessageStorageService.class);

    /**
     * 消息存储Map
     * Key: 监听器ID
     * Value: 该监听器接收到的消息列表
     */
    private final ConcurrentHashMap<String, List<String>> receivedMessages = new ConcurrentHashMap<>();

    /**
     * 保存消息到存储Map
     *
     * @param listenerId 监听器ID
     * @param message    消息内容
     */
    public void saveMessage(String listenerId, String message) {
        receivedMessages.computeIfAbsent(listenerId, k -> new ArrayList<>()).add(message);
        logger.debug("消息已保存到监听器 {}: {}", listenerId, message);
    }

    /**
     * 获取所有接收到的消息
     * <p>
     * 返回所有监听器接收到的消息记录
     *
     * @return 所有消息的Map（Key为监听器ID，Value为消息列表）
     */
    public Map<String, List<String>> getAllReceivedMessages() {
        return new HashMap<>(receivedMessages);
    }

    /**
     * 获取指定监听器的消息
     *
     * @param listenerId 监听器ID
     * @return 该监听器的消息列表，如果不存在则返回空列表
     */
    public List<String> getMessagesByListener(String listenerId) {
        return new ArrayList<>(receivedMessages.getOrDefault(listenerId, new ArrayList<>()));
    }

    /**
     * 清空指定监听器的消息
     *
     * @param listenerId 监听器ID
     */
    public void clearMessages(String listenerId) {
        receivedMessages.remove(listenerId);
        logger.info("已清空监听器 {} 的消息", listenerId);
    }

    /**
     * 清空所有消息
     */
    public void clearAllMessages() {
        receivedMessages.clear();
        logger.info("已清空所有监听器的消息");
    }
}

package com.envision.bunny.module.aws.web;

import com.envision.bunny.module.aws.application.MessageStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息存储控制器
 * <p>
 * 负责查询和管理监听器接收到的消息
 */
@RestController
@RequestMapping("/api/messages")
public class MessageStorageController {

    /**
     * 注入消息存储服务
     * 该服务负责存储和查询监听器接收到的消息
     */
    @Autowired(required = false)
    private MessageStorageService messageStorageService;

    /**
     * 获取所有接收到的消息
     * <p>
     * 该接口返回所有监听器接收到的消息记录
     * 用于查看和验证注解监听器的工作情况
     * <p>
     * 使用示例：
     * GET /api/messages
     *
     * @return 所有消息的Map（Key为监听器ID，Value为消息列表）
     */
    @GetMapping
    public Map<String, List<String>> getAllReceivedMessages() {
        return messageStorageService.getAllReceivedMessages();
    }

    /**
     * 获取指定监听器的消息
     * <p>
     * 该接口返回指定监听器ID对应的所有消息记录
     * <p>
     * 使用示例：
     * GET /api/messages/listener/basicListener
     *
     * @param listenerId 监听器ID
     * @return 该监听器的消息列表，如果不存在则返回空列表
     */
    @GetMapping("/listener/{listenerId}")
    public List<String> getMessagesByListener(@PathVariable String listenerId) {
        return messageStorageService.getMessagesByListener(listenerId);
    }

    /**
     * 清空指定监听器的消息
     * <p>
     * 该接口清空指定监听器ID对应的所有消息记录
     * <p>
     * 使用示例：
     * DELETE /api/messages/listener/basicListener
     *
     * @param listenerId 监听器ID
     * @return 操作结果描述信息
     */
    @DeleteMapping("/listener/{listenerId}")
    public String clearMessages(@PathVariable String listenerId) {
        messageStorageService.clearMessages(listenerId);
        return String.format("已清空监听器 %s 的消息", listenerId);
    }

    /**
     * 清空所有消息
     * <p>
     * 该接口清空所有监听器的消息记录
     * <p>
     * 使用示例：
     * DELETE /api/messages
     *
     * @return 操作结果描述信息
     */
    @DeleteMapping
    public String clearAllMessages() {
        messageStorageService.clearAllMessages();
        return "已清空所有监听器的消息";
    }
}

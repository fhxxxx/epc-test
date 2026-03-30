package com.envision.bunny.module.aws.application.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户消息实体类
 *
 * 该类用于演示@SqsListener监听器对JSON对象的自动反序列化功能
 * SQS监听器会自动将JSON格式的消息转换为UserMessage对象
 *
 * @author example
 * @version 1.0.0
 */
@Setter
@Getter
public class UserMessage {

    /**
     * 消息ID
     */
    private String id;

    /**
     * 用户名称
     */
    private String name;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 无参构造函数（JSON反序列化需要）
     */
    public UserMessage() {
    }

    /**
     * 全参构造函数
     *
     * @param id 消息ID
     * @param name 用户名称
     * @param content 消息内容
     * @param timestamp 时间戳
     */
    public UserMessage(String id, String name, String content, LocalDateTime timestamp) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.timestamp = timestamp;
    }

    // Getters and Setters

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserMessage that = (UserMessage) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(name, that.name) &&
                Objects.equals(content, that.content) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, content, timestamp);
    }

    @Override
    public String toString() {
        return "UserMessage{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}

package com.mirrors.core.enums;

import lombok.Getter;

/**
 * 消息类型
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 16:08
 */
public enum MessageType {
    /**
     * 类型 0 表示请求消息
     * () 默认调用构造器
     */
    REQUEST((byte) 0),

    /**
     * 类型 1 表示响应消息
     */
    RESPONSE((byte) 1),

    /**
     * 类型 2 表示心跳检查请求
     */
    HEARTBEAT_REQUEST((byte) 2),

    /**
     * 类型 3 表示心跳检查响应
     */
    HEARTBEAT_RESPONSE((byte) 3);

    /**
     * 消息类型，字节标识
     */
    @Getter
    private final byte type;

    /**
     * 构造器，初始化type类型
     *
     * @param type
     */
    MessageType(Byte type) {
        this.type = type;
    }

    /**
     * 根据类型返回枚举
     *
     * @param type
     * @return
     */
    public static MessageType getByType(Byte type) {
        for (MessageType messageType : MessageType.values()) {
            if (messageType.getType() == type) {
                return messageType;
            }
        }
        // 找不到符合的消息类型，抛出异常，利用String.format占位符
        throw new IllegalArgumentException(String.format("The message type %s is illegal.", type));
    }
}

package com.mirrors.core.enums;

import lombok.Getter;

/**
 * 消息状态类
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 16:21
 */
public enum MessageStatus {
    /**
     * 成功
     */
    SUCCESS((byte) 0),

    /**
     * 失败
     */
    FAIL((byte) 1);

    /**
     * 消息类型
     */
    @Getter
    private final byte type;

    /**
     * 构造函数
     *
     * @param type
     */
    MessageStatus(byte type) {
        this.type = type;
    }

    /**
     * 是否消息类型是否为 成功
     *
     * @param type
     * @return
     */
    public static boolean isSuccess(byte type) {
        return MessageStatus.SUCCESS.type == type;
    }
}

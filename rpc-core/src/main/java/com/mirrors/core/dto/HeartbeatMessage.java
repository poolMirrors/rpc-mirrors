package com.mirrors.core.dto;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 心跳检测机制 信息
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 21:20
 */
@Data
@Builder
public class HeartbeatMessage implements Serializable {

    /**
     * 消息
     */
    private String message;
}

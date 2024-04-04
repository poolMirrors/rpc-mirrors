package com.mirrors.core.constants;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 一些涉及rpc的常量
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 15:42
 */
public class RpcConstant {

    /**
     * 魔数，用来判断是否无效数据包
     */
    public static final byte[] MAGIC_NUM = new byte[]{(byte) 't', (byte) 'z', (byte) 'j', (byte) '6'};

    /**
     * 版本号
     */
    public static final byte VERSION = 1;

    /**
     * 特殊消息类型ping
     */
    public static final String PING = "ping";

    /**
     * 特殊消息类型pong
     */
    public static final String PONG = "pong";

    /**
     * 消息的序列id
     */
    public static final AtomicInteger sequenceId = new AtomicInteger();

    /**
     * 返回序列号
     *
     * @return
     */
    public static int getSequenceId() {
        return sequenceId.getAndIncrement();
    }
}

package com.mirrors.core.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * LengthFieldBasedFrameDecoder解码器自定义长度解决TCP粘包黏包问题
 * 本质上是ChannelHandler，一个处理入站事件的ChannelHandler
 * 用定长字节表示接下来数据的长度
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 14:51
 */
public class RpcFrameDecoder extends LengthFieldBasedFrameDecoder {

    /**
     * 无参构造
     * 数据帧的最大长度，消息长度字段的偏移字节数，长度域所占的字节数
     */
    public RpcFrameDecoder() {
        super(1024, 12, 4);
    }

    /**
     * 构造函数
     *
     * @param maxFrameLength    数据帧的最大长度
     * @param lengthFieldOffset 长度域的偏移字节数
     * @param lengthFieldLength 长度域所占的字节数
     */
    public RpcFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }
}

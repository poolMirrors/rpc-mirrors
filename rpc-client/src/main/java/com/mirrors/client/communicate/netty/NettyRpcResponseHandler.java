package com.mirrors.client.communicate.netty;

import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.bean.RpcMessageHeader;
import com.mirrors.core.constants.RpcConstant;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.enums.MessageType;
import com.mirrors.core.enums.SerializerType;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端响应信息处理器，指定RpcMessage类型
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 17:28
 */
@Slf4j
public class NettyRpcResponseHandler extends SimpleChannelInboundHandler<RpcMessage> {

    /**
     * 保存 还未处理的请求
     */
    public static final Map<Integer, Promise<RpcMessage>> UNPROCESSED_RESPONSE = new ConcurrentHashMap<>();

    /**
     * 触发读
     *
     * @param channelHandlerContext
     * @param rpcMessage
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage) throws Exception {
        try {
            MessageType messageType = MessageType.getByType(rpcMessage.getRpcMessageHeader().getMessageType());
            // 判断接受的消息类型 是rpc请求 还是心跳检测
            if (messageType == MessageType.RESPONSE) {
                // rpc返回请求
                int sequenceId = rpcMessage.getRpcMessageHeader().getSequenceId();
                // 拿到 还未处理的 promise 对象【取出promise，设置返回信息】
                Promise<RpcMessage> promise = UNPROCESSED_RESPONSE.remove(sequenceId);
                if (promise != null) {
                    // 强转RpcResponse信息
                    RpcResponse rpcResponse = (RpcResponse) rpcMessage.getRpcMessageBody();
                    Exception exception = rpcResponse.getExceptionValue();
                    // 根据 远程调用返回的结果是否出错 设置promise的结果！
                    if (exception != null) {
                        promise.setFailure(exception);
                    } else {
                        promise.setSuccess(rpcMessage);
                    }
                }

            } else if (messageType == MessageType.HEARTBEAT_RESPONSE) {
                // 心跳检测
                log.debug("heartbeat message: {}.", rpcMessage.getRpcMessageBody());
            }
        } finally {
            // 释放资源
            ReferenceCountUtil.release(rpcMessage);
        }
    }

    /**
     * 触发写空闲，当检测到写空闲发生自动发送一个心跳检测数据包
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // 判断是否触发空闲世界
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            // 如果触发写空闲，发送心跳检测
            if (idleStateEvent.state() == IdleState.WRITER_IDLE) {
                log.warn("write idle happen [{}].", ctx.channel().remoteAddress());
                // 构建心跳检测信息
                RpcMessageHeader header = RpcMessageHeader.createBySerializer(SerializerType.KRYO.name());
                header.setMessageType(MessageType.HEARTBEAT_REQUEST.getType());

                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setRpcMessageHeader(header);
                rpcMessage.setRpcMessageBody(RpcConstant.PING);
                // 发送心跳检测请求，异常时关闭
                ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }

        } else {
            // 没有触发空闲，放行
            super.userEventTriggered(ctx, evt);
        }
    }

    /**
     * 捕获到异常时
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("client catch exception: ", cause);
        cause.printStackTrace();
        ctx.close();
    }
}

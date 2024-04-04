package com.mirrors.server.communicate.netty;

import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.bean.RpcMessageHeader;
import com.mirrors.core.constants.RpcConstant;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.enums.MessageStatus;
import com.mirrors.core.enums.MessageType;
import com.mirrors.core.factory.SingletonFactory;
import com.mirrors.server.handler.RpcRequestHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 基于 netty 的请求消息入站处理器
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 15:29
 */
@Slf4j
public class NettyRpcRequestHandler extends SimpleChannelInboundHandler {

    /**
     * 线程池，数组阻塞队列
     */
    private static final ThreadPoolExecutor threadPool = new ThreadPoolExecutor(10, 10, 60L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(10000));
    /**
     * 反射调用 服务的本地缓存，返回结果
     */
    private final RpcRequestHandler rpcRequestHandler;

    public NettyRpcRequestHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    /**
     * 当收到对方发来的数据后，就会触发，参数o 就是发来的信息，
     * 利用线程池处理每个任务，不阻塞！
     *
     * @param channelHandlerContext
     * @param o
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        // 转为RpcMessage
        RpcMessage request = (RpcMessage) o;
        // 线程池，不阻塞执行
        threadPool.submit(() -> {
            try {
                RpcMessage response = new RpcMessage();
                // 获取消息头
                RpcMessageHeader header = request.getRpcMessageHeader();
                MessageType messageType = MessageType.getByType(header.getMessageType());
                log.debug("The message received by the server is: {}", request.getRpcMessageBody());
                // 判断消息类型
                if (messageType == MessageType.HEARTBEAT_REQUEST) {
                    //（1）如果是心跳检测请求
                    header.setMessageType(MessageType.HEARTBEAT_RESPONSE.getType());
                    header.setMessageStatus(MessageStatus.SUCCESS.getType());
                    // 设置响应头信息
                    response.setRpcMessageHeader(header);
                    response.setRpcMessageBody(RpcConstant.PONG);
                } else {
                    //（2）不是心跳信息，rpc信息处理
                    RpcRequest rpcRequest = (RpcRequest) request.getRpcMessageBody();
                    RpcResponse rpcResponse = new RpcResponse();
                    // 设置信息类型
                    header.setMessageType(MessageType.RESPONSE.getType());
                    // 反射调用【重点】
                    try {
                        Object result = rpcRequestHandler.invokeService(rpcRequest);
                        rpcResponse.setReturnValue(result);
                        header.setMessageStatus(MessageStatus.SUCCESS.getType());
                    } catch (Exception e) {
                        log.error("The service [{}], the method [{}] invoke failed!", rpcRequest.getServiceName(), rpcRequest.getMethodName());
                        // 压缩错误信息，不然堆栈信息过多，导致报错
                        rpcResponse.setExceptionValue(new RuntimeException("Error in remote procedure call, " + e.getMessage()));
                        header.setMessageStatus(MessageStatus.FAIL.getType());
                    }
                    // 设置响应信息
                    response.setRpcMessageHeader(header);
                    response.setRpcMessageBody(rpcResponse);
                }
                log.debug("response: {}.", response);
                // 传递下一个处理器，如果写响应失败，将通道关闭
                channelHandlerContext.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);

            } finally {
                // 释放资源
                ReferenceCountUtil.release(o);
            }
        });
    }

    /**
     * 当服务端 设定时间内 没有接收到客户端发送的读事件，就触发 userEventTriggered 方法。
     *
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            // 如果触发的空闲事件是 读空闲，关闭Channel
            if (state == IdleState.READER_IDLE) {
                log.warn("idle check happen");
                ctx.close();
            }
        } else {
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
        log.error("server catch exception: ", cause);
        cause.printStackTrace();
        ctx.close();
    }
}

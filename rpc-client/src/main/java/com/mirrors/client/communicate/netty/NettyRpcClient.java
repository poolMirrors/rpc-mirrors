package com.mirrors.client.communicate.netty;

import com.mirrors.client.bean.RequestMetadata;
import com.mirrors.client.communicate.RpcClient;
import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.codec.RpcFrameDecoder;
import com.mirrors.core.codec.SharableRpcMessageCodec;
import com.mirrors.core.factory.SingletonFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 基于netty的客户端
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 17:06
 */
@Slf4j
public class NettyRpcClient implements RpcClient {

    /**
     * netty客户端 启动类
     */
    private final Bootstrap bootstrap;

    /**
     * 保存和获取 连接的对应Channel对象
     * <p>
     * key=ip和端口
     * <p>
     * value=channel
     */
    private final NettyChannelCache channelCache;

    /**
     * 无参构造函数，初始化客户端（还未连接）
     */
    public NettyRpcClient() {
        bootstrap = new Bootstrap();
        channelCache = SingletonFactory.getInstance(NettyChannelCache.class);
        // 事件循环对象组，每一个事件循环对象对应一个线程（维护一个 Selector），用来处理 channel 上的 io 事件
        NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        // 配置启动类
        bootstrap
                .group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                // todo: 最后改为5000，修改为500000方便debug
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        // 超过 15s 内如果没有向服务器写数据，触发 写空闲
                        // todo: 写空闲被注释，方便debug
                        pipeline.addLast(new IdleStateHandler(0, 15, 0, TimeUnit.SECONDS));
                        // 添加 粘包拆包 解码器
                        pipeline.addLast(new RpcFrameDecoder());
                        // 添加 编解码器
                        pipeline.addLast(new SharableRpcMessageCodec());
                        // 添加 rpc响应消息处理器
                        pipeline.addLast(new NettyRpcResponseHandler());
                    }
                });
    }

    /**
     * 发送消息，利用promise记录 每次发送后等待回应的channel？
     *
     * @param requestMetadata
     * @return
     */
    @Override
    public RpcMessage sendRpcRequest(RequestMetadata requestMetadata) {
        try {
            // 创建 接受响应结果的 promise【Netty提供】
            Promise<RpcMessage> promise;
            // 获取连接的channel对象 =》
            Channel channel = getChannel(new InetSocketAddress(requestMetadata.getServerIp(), requestMetadata.getServerPort()));
            if (channel.isActive()) {
                //（1）连接成功，使用 promise 接受结果（指定 执行完成通知的线程）
                promise = new DefaultPromise<>(channel.eventLoop());
                // 获取 序列号id
                int sequenceId = requestMetadata.getRpcMessage().getRpcMessageHeader().getSequenceId();
                // 保存 还没处理请求结果的 promise【重点】
                NettyRpcResponseHandler.UNPROCESSED_RESPONSE.put(sequenceId, promise);
                // 发送数据，监听发送状态
                channel.writeAndFlush(requestMetadata.getRpcMessage()).addListener((ChannelFutureListener) future -> {
                    if (future.isSuccess()) {
                        log.debug("the client send message successfully: [{}]", requestMetadata.getRpcMessage());
                    } else {
                        future.channel().close(); // 发送失败，关闭channel
                        promise.setFailure(future.cause());
                        log.error("the client send message fail: ", future.cause());
                    }
                });

                //（2）获取超时时间
                Integer timeout = requestMetadata.getTimeout();
                // 等待结果返回，让出cpu资源，同步阻塞调用线程main，其他线程去执行获取操作（eventLoop）
                if (timeout == null || timeout <= 0) {
                    // 如果没有指定超时时间，则 await 直到 promise 完成
                    promise.await();
                } else {
                    // 在超时时间内等待结果返回
                    boolean isSuccess = promise.await(timeout, TimeUnit.MILLISECONDS);
                    if (!isSuccess) {
                        promise.setFailure(new TimeoutException(String.format("remote call exceeded timeout of %dms.", timeout)));
                    }
                }

                //（3）已经等到响应结果，如果调用成功立即返回
                if (promise.isSuccess()) {
                    return promise.getNow();
                } else {
                    throw new RuntimeException(promise.cause());
                }

            } else {
                throw new RuntimeException("the channel is inactivate");
            }

        } catch (Exception e) {
            throw new RuntimeException("error occur while sending: ", e);
        }
    }

    /**
     * 获取 该连接的channel对象；
     * 如果没有找到连接的channel对象，说明还没进行连接；先进行连接，保存后返回channel
     *
     * @param socketAddress
     * @return
     */
    public Channel getChannel(InetSocketAddress socketAddress) {
        // 【重用Channel】，如果多次调用，直接从Map取即可，无需再次创建
        Channel channel = channelCache.get(socketAddress);
        if (channel == null) {
            // 如果没有找到连接的channel对象，说明还没进行连接；先进行连接 =》
            channel = connect(socketAddress);
            // 连接成功后，保存连接的channel对象
            channelCache.set(socketAddress, channel);
        }
        return channel;
    }

    /**
     * 根据 ip:port 与服务端进行连接，返回channel；
     * 使用 CompletableFuture 异步接受（重点知识）
     *
     * @param socketAddress
     * @return
     */
    public Channel connect(InetSocketAddress socketAddress) {
        try {
            CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
            // 客户端开始连接，添加监听器，接收到响应回调！
            bootstrap.connect(socketAddress).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.debug("the client successfully connected to server [{}]", socketAddress.toString());
                    // 传入结果future.channel()，表明已经执行完
                    completableFuture.complete(future.channel());
                } else {
                    throw new RuntimeException(String.format("the client failed to connect to [%s].", socketAddress.toString()));
                }
            });
            // 阻塞等待 future 返回 连接结果
            Channel channel = completableFuture.get();
            // 添加 异步 关闭之后的操作
            channel.closeFuture().addListener(future -> {
                log.info("the client disconnected from server [{}].", socketAddress.toString());
            });
            return channel;

        } catch (Exception e) {
            throw new RuntimeException("error occur while connecting: ", e);
        }
    }
}

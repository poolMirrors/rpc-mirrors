package com.mirrors.server.communicate.netty;

import com.mirrors.core.codec.RpcFrameDecoder;
import com.mirrors.core.codec.SharableRpcMessageCodec;
import com.mirrors.server.communicate.RpcServer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * 基于netty通信
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/14 16:24
 */
@Slf4j
public class NettyRpcServer implements RpcServer {
    @Override
    public void start(Integer port) {
        // boss 处理 accept事件，worker 处理读写事件
        NioEventLoopGroup boss = new NioEventLoopGroup();
        NioEventLoopGroup worker = new NioEventLoopGroup();

        try {
            // 获取到本机ip地址
            InetAddress localHost = InetAddress.getLocalHost();
            // 配置参数
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据块，减少网络传输
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开始 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢
                    .option(ChannelOption.SO_BACKLOG, 128)
                    // 日志记录
                    .handler(new LoggingHandler(LogLevel.DEBUG))
                    // 添加处理器
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            // 超过 30s 内如果没有从客户端读数据，触发 读空闲
                            // todo: 读空闲被注释，方便debug
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            // 添加 粘包拆包 解码器
                            pipeline.addLast(new RpcFrameDecoder());
                            // 添加 编解码器
                            pipeline.addLast(new SharableRpcMessageCodec());
                            // 添加 rpc请求消息处理器
                            pipeline.addLast(new NettyRpcRequestHandler());
                        }
                    });
            // 绑定端口，同步等到绑定成功
            ChannelFuture channelFuture = serverBootstrap.bind(localHost, port).sync();
            log.debug("Rpc server add {} started on the port {}.", localHost, port);
            // 同步等待 服务端 监听 端口关闭，当channel没有断开时，线程阻塞，直到断开为止
            channelFuture.channel().closeFuture().sync();

        } catch (Exception e) {
            log.error("Error occurred while starting the rpc service.", e);
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }
}

package com.mirrors.client.communicate.netty;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存和获取 连接的对应Channel对象
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 20:59
 */
public class NettyChannelCache {

    /**
     * 存储channel；
     * key = ip:port（String类型）；
     * value = channel对象
     */
    private final Map<String, Channel> channelMap = new ConcurrentHashMap<>();

    /**
     * 根据ip和端口，获取连接的channel对象
     *
     * @param ip
     * @param port
     * @return
     */
    public Channel get(String ip, Integer port) {
        String key = ip + ":" + port;
        // 判断 ip:port 是否已经连接
        if (channelMap.containsKey(key)) {
            // 获取对应channel
            Channel channel = channelMap.get(key);
            if (channel != null && channel.isActive()) {
                // 如果channel存在且活跃
                return channel;
            } else {
                // 否则从缓存中删除
                channelMap.remove(key);
            }
        }
        return null;
    }

    /**
     * 根据InetSocketAddress，获取连接的channel对象
     *
     * @param socketAddress
     * @return
     */
    public Channel get(InetSocketAddress socketAddress) {
        return get(socketAddress.getHostName(), socketAddress.getPort());
    }

    /**
     * 根据ip和端口，保存连接的channel对象
     *
     * @param ip
     * @param port
     * @param channel
     */
    public void set(String ip, Integer port, Channel channel) {
        String key = ip + ":" + port;
        channelMap.put(key, channel);
    }

    /**
     * 根据InetSocketAddress，保存连接的channel对象
     *
     * @param socketAddress
     * @param channel
     */
    public void set(InetSocketAddress socketAddress, Channel channel) {
        set(socketAddress.getHostName(), socketAddress.getPort(), channel);
    }
}

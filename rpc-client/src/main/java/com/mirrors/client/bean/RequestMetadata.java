package com.mirrors.client.bean;

import com.mirrors.core.bean.RpcMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 请求的元数据类，远程调用时并非将此类直接发送；
 * 而是发送 RpcMessage属性 或 RpcMessage属性的一部分；
 * 其他属性是传递给 RpcClient 方便处理
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 9:27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestMetadata {
    /**
     * 发送的消息（消息头+消息正文）
     */
    private RpcMessage rpcMessage;

    /**
     * 远程服务提供的地址
     */
    private String serverIp;

    /**
     * 远程服务提供的端口
     */
    private Integer serverPort;

    /**
     * 调用超时时间
     */
    private Integer timeout;
}

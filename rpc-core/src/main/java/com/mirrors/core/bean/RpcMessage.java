package com.mirrors.core.bean;

import lombok.Data;

/**
 * 网路传输的协议信息类
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 15:32
 */
@Data
public class RpcMessage {

    /**
     * 消息头
     */
    private RpcMessageHeader rpcMessageHeader;

    /**
     * 消息体，取值可以为 com.mirrors.core.dto 下的实体类
     */
    private Object rpcMessageBody;
}

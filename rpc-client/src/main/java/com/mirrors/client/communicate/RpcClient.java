package com.mirrors.client.communicate;

import com.mirrors.client.bean.RequestMetadata;
import com.mirrors.core.bean.RpcMessage;

/**
 * 客户端连接，发起远程调用
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/15 10:54
 */
public interface RpcClient {

    /**
     * 发起远程调用
     *
     * @param requestMetadata
     * @return
     */
    RpcMessage sendRpcRequest(RequestMetadata requestMetadata);
}

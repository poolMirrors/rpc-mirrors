package com.mirrors.core.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * rpc完成后的返回信息
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 21:20
 */
@Data
public class RpcResponse implements Serializable {

    /**
     * 调用成功后的返回信息
     */
    private Object returnValue;

    /**
     * 调用发生异常时的信息，注意不要直接把异常赋值给exceptionValue，容易导致超出数据包长长度范围
     */
    private Exception exceptionValue;

}

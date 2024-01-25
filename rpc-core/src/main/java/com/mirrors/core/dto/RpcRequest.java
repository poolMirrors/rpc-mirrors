package com.mirrors.core.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 21:20
 */
@Data
public class RpcRequest implements Serializable {

    /**
     * 服务名称 = 服务名-版本号
     */
    private String serviceName;

    /**
     * 要调用的方法名
     */
    private String methodName;

    /**
     * 方法参数类型
     */
    private Class<?>[] paramTypes;

    /**
     * 方法参数值
     */
    private Object[] paramValues;

}

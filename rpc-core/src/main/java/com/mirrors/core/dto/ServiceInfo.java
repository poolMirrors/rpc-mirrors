package com.mirrors.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * rpc中服务提供方的服务信息
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 21:20
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInfo implements Serializable {

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 服务名
     * 服务名称 = 服务名-版本号
     */
    private String serviceName;

    /**
     * 版本号
     * 服务名称 = 服务名-版本号
     */
    private String version;

    /**
     * 服务端地址
     */
    private String ip;

    /**
     * 服务端端口
     */
    private Integer port;
}

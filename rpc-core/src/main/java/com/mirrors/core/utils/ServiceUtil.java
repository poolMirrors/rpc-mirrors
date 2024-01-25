package com.mirrors.core.utils;

import com.google.gson.Gson;
import com.mirrors.core.dto.ServiceInfo;

import java.util.Collections;
import java.util.Map;

/**
 * service 工具类，用于 ServiceInfo对象 与 Map 互相转换
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/12 16:30
 */
public class ServiceUtil {

    /**
     * 协助 ServiceInfo对象 和 Map 互转
     */
    public static final Gson gson = new Gson();

    /**
     * 根据 服务名称-版本号 生成注册服务的 key
     *
     * @param serviceName
     * @param version
     * @return
     */
    public static String getServiceKey(String serviceName, String version) {
        return String.join("-", serviceName, version);
    }

    /**
     * 将 serviceInfo 对象转换为 map
     *
     * @param serviceInfo
     * @return
     */
    public static Map toMap(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return Collections.emptyMap();
        }
        Map map = gson.fromJson(gson.toJson(serviceInfo), Map.class);
        // 由于port是Integer类型，所以要单独加入map
        map.put("port", serviceInfo.getPort().toString());
        return map;
    }

    /**
     * 将 map 对象转换为 serviceInfo
     *
     * @param map
     * @return
     */
    public static ServiceInfo toServiceInfo(Map map) {
        // 由于在toMap时，port的值转为了String类型，所以要重新put一个Integer类型
        map.put("port", Integer.parseInt(map.get("port").toString()));
        ServiceInfo serviceInfo = gson.fromJson(gson.toJson(map), ServiceInfo.class);
        return serviceInfo;
    }
}

package com.mirrors.core.serializer;

import com.mirrors.core.enums.SerializerType;
import com.mirrors.core.serializer.hessian.HessianSerializer;
import com.mirrors.core.serializer.jdk.JdkSerializer;
import com.mirrors.core.serializer.json.JsonSerializer;
import com.mirrors.core.serializer.kryo.KryoSerializer;
import com.mirrors.core.serializer.protostuff.ProtostuffSerializer;

/**
 * 根据 序列化类型 找到 序列化器
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:35
 */
public class SerializerFactory {

    /**
     * 根据 序列化类型 找到 序列化器
     *
     * @param serializerType
     * @return
     */
    public static Serializer getSerializer(SerializerType serializerType) {
        switch (serializerType) {
            case JDK:
                return new JdkSerializer();
            case JSON:
                return new JsonSerializer();
            case KRYO:
                return new KryoSerializer();
            case PROTOSTUFF:
                return new ProtostuffSerializer();
            case HESSIAN:
                return new HessianSerializer();
            default:
                // 找不到对应算法就报错
                throw new IllegalArgumentException(String.format("The serialization type %s is illegal.", serializerType.name()));
        }
    }
}

package com.mirrors.core.enums;

import lombok.Getter;

/**
 * 序列化类型
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 16:28
 */
public enum SerializerType {
    /**
     * JDK 序列化算法
     */
    JDK((byte) 0),

    /**
     * JSON 序列化算法
     */
    JSON((byte) 1),

    /**
     * HESSIAN 序列化算法
     */
    HESSIAN((byte) 2),

    /**
     * KRYO 序列化算法
     */
    KRYO((byte) 3),

    /**
     * PROTOSTUFF 序列化算法
     */
    PROTOSTUFF((byte) 4);

    /**
     * 序列化类型
     */
    @Getter
    private final byte type;

    /**
     * 构造函数
     *
     * @param type
     */
    SerializerType(byte type) {
        this.type = type;
    }

    /**
     * 通过序列化类型获取序列化算法枚举类
     *
     * @param type 类型
     * @return 枚举类型
     */
    public static SerializerType getByType(byte type) {
        for (SerializerType serializerType : SerializerType.values()) {
            if (serializerType.getType() == type) {
                return serializerType;
            }
        }
        // HESSIAN 作为默认序列化算法
        return HESSIAN;
    }

    /**
     * 通过序列化算法名 获取序列化算法枚举类
     *
     * @param serializerName 类型名称
     * @return 枚举类型
     */
    public static SerializerType getByName(String serializerName) {
        for (SerializerType serializerType : SerializerType.values()) {
            // 不考虑大小写
            if (serializerType.name().equalsIgnoreCase(serializerName)) {
                return serializerType;
            }
        }
        // HESSIAN 作为默认序列化算法
        return HESSIAN;
    }
}

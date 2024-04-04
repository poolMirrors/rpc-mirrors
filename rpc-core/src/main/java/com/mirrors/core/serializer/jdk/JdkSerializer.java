package com.mirrors.core.serializer.jdk;

import com.mirrors.core.serializer.Serializer;

import java.io.*;

/**
 * jdk序列化
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:32
 */
public class JdkSerializer implements Serializer {

    /**
     * 序列化
     *
     * @param data
     * @param <T>
     * @return
     */
    @Override
    public <T> byte[] serialize(T data) {
        try {
            // 初始化ByteArrayOutputStream
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // 对象的序列化流，把对象转成字节数据的输出到ByteArrayOutputStream保存
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            // 将 data 写入ByteArrayOutputStream
            objectOutputStream.writeObject(data);
            // 利用 ByteArrayOutputStream 转为字节数组返回
            return byteArrayOutputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("jdk serialize fail", e);
        }
    }

    /**
     * 反序列化
     *
     * @param clazz
     * @param bytes
     * @param <T>
     * @return
     */
    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try {
            // 读入bytes数组
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            // 利用 ObjectInputStream 转为 Object
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            // 类型转换，返回
            return (T) objectInputStream.readObject();

        } catch (Exception e) {
            throw new RuntimeException("jdk serialize fail", e);
        }
    }
}

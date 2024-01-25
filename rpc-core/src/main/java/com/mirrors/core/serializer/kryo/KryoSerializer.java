package com.mirrors.core.serializer.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:33
 */
public class KryoSerializer implements Serializer {

    /**
     * kryo不是线程安全
     * 利用ThreadLocal为每一个线程初始化时，都创建返回属于自己的一个kryo
     */
    private final ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 支持 RpcRequest 和 RpcResponse 两个类的序列化和反序列化
        kryo.register(RpcRequest.class);
        kryo.register(RpcResponse.class);
        return kryo;
    });

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
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // 利用Kryo提供的 Output 来接受
            Output output = new Output(byteArrayOutputStream);
            // 从 kryoThreadLocal 中取出本线程的 kryo 对象
            Kryo kryo = kryoThreadLocal.get();
            // 将data写入output
            kryo.writeObject(output, data);
            // 使用后，remove移除本线程对象
            kryoThreadLocal.remove();
            // 使用output转为 byte数组 返回
            return output.toBytes();
        } catch (Exception e) {
            throw new RuntimeException("kryo serialize fail", e);
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
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            // 利用Kryo提供的 Input 来接受
            Input input = new Input(byteArrayInputStream);
            Kryo kryo = kryoThreadLocal.get();
            // kryo从input中读取对象
            T object = kryo.readObject(input, clazz);
            // 先删除，再返回
            kryoThreadLocal.remove();
            return object;
        } catch (Exception e) {
            throw new RuntimeException("kryo serialize fail", e);
        }
    }
}

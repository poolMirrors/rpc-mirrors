package com.mirrors.core.serializer.hessian;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import com.mirrors.core.serializer.Serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Hessian序列化
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:32
 */
public class HessianSerializer implements Serializer {

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
            // 保存到 ByteArrayOutputStream
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            // 利用 HessianSerializerOutput 将data写入 ByteArrayOutputStream
            HessianSerializerOutput hessianSerializerOutput = new HessianSerializerOutput(byteArrayOutputStream);
            hessianSerializerOutput.writeObject(data);
            // 注意要 flush 刷新
            hessianSerializerOutput.flush();
            return byteArrayOutputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("hessian serialize fail", e);
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
            // 读入 bytes
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
            // 读入 byteArrayInputStream，转为Object
            HessianSerializerInput hessianSerializerInput = new HessianSerializerInput(byteArrayInputStream);
            return (T) hessianSerializerInput.readObject();

        } catch (IOException e) {
            throw new RuntimeException("hessian serialize fail", e);
        }
    }
}

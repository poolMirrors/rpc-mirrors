package com.mirrors.core.serializer.protostuff;

import com.mirrors.core.serializer.Serializer;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:34
 */
@Deprecated
public class ProtostuffSerializer implements Serializer {

    /**
     * 提前分配Buffer，避免每次进行序列化都需要重新分配 buffer 内存空间
     * 默认 512B
     */
    private final LinkedBuffer LINKEDBUFFER = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE);

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
            // 序列化对象的结构
            Schema schema = RuntimeSchema.getSchema(data.getClass());
            return ProtostuffIOUtil.toByteArray(data, schema, LINKEDBUFFER);

        } catch (Exception e) {
            throw new RuntimeException("protostuff serialize fail", e);
        } finally {
            // 序列化要重置 LINKEDBUFFER
            LINKEDBUFFER.clear();
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
            Schema<T> schema = RuntimeSchema.getSchema(clazz);
            // schema.newMessage()底层使用的反射创建？
            T object = schema.newMessage();
            // 合并另一个消息对象中的字段到当前消息对象中，如果当前消息对象中已有相同的字段，则不会覆盖它们
            ProtostuffIOUtil.mergeFrom(bytes, object, schema);
            return object;

        } catch (Exception e) {
            throw new RuntimeException("protostuff serialize fail", e);
        }
    }
}

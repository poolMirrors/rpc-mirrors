package com.mirrors.core.serializer.json;

import com.google.gson.*;
import com.mirrors.core.serializer.Serializer;
import lombok.SneakyThrows;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 19:33
 */
public class JsonSerializer implements Serializer {

    /**
     * 内部类：自定义 JavaClass 对象序列化，解决 Gson 无法序列化 Class 信息
     * 1.实现 JsonSerializer<Class<?>> 接口； Class<?>类型
     * 2.实现 JsonDeserializer<Class<?>> 接口； Class<?>类型
     */
    static class ClassCodec implements com.google.gson.JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

        @SneakyThrows
        @Override
        public Class<?> deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            // 根据全限定类名 json -> class
            String name = jsonElement.getAsString();
            return Class.forName(name);
        }

        @Override
        public JsonElement serialize(Class<?> aClass, Type type, JsonSerializationContext jsonSerializationContext) {
            // 根据全限定类名 class -> json
            return new JsonPrimitive(aClass.getName());
        }
    }

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
            // 利用 registerTypeAdapter 添加 特定对Class类型的序列化类ClassCodec
            Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
            // 对象 -> json
            String json = gson.toJson(data);
            return json.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("json serialize fail", e);
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
            // 利用 registerTypeAdapter 添加 特定对Class类型的序列化类ClassCodec
            Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
            // 将 bytes 转为 String
            String json = new String(bytes, StandardCharsets.UTF_8);
            // 利用 gson 将 json（根据clazz）转为特定对象
            return gson.fromJson(json, clazz);
        } catch (JsonSyntaxException e) {
            throw new RuntimeException("json serialize fail", e);
        }
    }
}

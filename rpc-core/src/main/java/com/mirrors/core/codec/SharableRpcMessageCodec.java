package com.mirrors.core.codec;

import com.mirrors.core.bean.RpcMessage;
import com.mirrors.core.bean.RpcMessageHeader;
import com.mirrors.core.constants.RpcConstant;
import com.mirrors.core.dto.RpcRequest;
import com.mirrors.core.dto.RpcResponse;
import com.mirrors.core.enums.MessageType;
import com.mirrors.core.enums.SerializerType;
import com.mirrors.core.serializer.Serializer;
import com.mirrors.core.serializer.SerializerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;

import java.util.Arrays;
import java.util.List;

/**
 * 注解Sharable标志共享
 * 消息编码解码器，前提要有 {@link com.mirrors.core.codec.RpcFrameDecoder} 进行粘包半包处理
 * --------------------------------------------------------------------
 * | 魔数 (4byte) | 版本号 (1byte)  | 序列化算法 (1byte) | 消息类型 (1byte) |
 * --------------------------------------------------------------------
 * |    状态类型 (1byte)  |    消息序列号 (4byte)   |    消息长度 (4byte)   |
 * --------------------------------------------------------------------
 * |                        消息内容 (不固定长度)                         |
 * --------------------------------------------------------------------
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 15:23
 */
@ChannelHandler.Sharable
public class SharableRpcMessageCodec extends MessageToMessageCodec {

    /**
     * 编码，将RpcMessage转为ByteBuf
     *
     * @param channelHandlerContext
     * @param object
     * @param list
     * @throws Exception
     */
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object object, List list) throws Exception {
        // 将object类型转换，拿到 消息头 和 消息体
        RpcMessage message = (RpcMessage) object;
        RpcMessageHeader header = message.getRpcMessageHeader();
        Object body = message.getRpcMessageBody();

        // 拿到ByteBuf
        ByteBuf byteBuf = channelHandlerContext.alloc().buffer();

        // 根据 消息头 设置
        byteBuf.writeBytes(header.getMagicNum());
        byteBuf.writeByte(header.getVersion());
        byteBuf.writeByte(header.getSerializerType());
        byteBuf.writeByte(header.getMessageType());
        byteBuf.writeByte(header.getMessageStatus());
        byteBuf.writeInt(header.getSequenceId());

        // 计算消息长度（获取序列化算法，先序列化，再设置 消息头的长度字段）
        SerializerType serializerType = SerializerType.getByType(header.getSerializerType());
        Serializer serializer = SerializerFactory.getSerializer(serializerType);
        byte[] bytes = serializer.serialize(body);
        header.setLength(bytes.length);

        // 消息体长度 和 消息正文 依次写入ByteBuf
        byteBuf.writeInt(bytes.length);
        byteBuf.writeBytes(bytes);

        // 传递下一个handler
        list.add(byteBuf);
    }

    /**
     * 解码,将ByteBuf转为RpcMessage
     *
     * @param channelHandlerContext
     * @param object
     * @param list
     * @throws Exception
     */
    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, Object object, List list) throws Exception {
        // 将object类型转换ByteBuf
        ByteBuf byteBuf = (ByteBuf) object;

        // 4字节 魔数，判断是否符合协议要求
        int magicNumLength = RpcConstant.MAGIC_NUM.length;
        byte[] magicNum = new byte[magicNumLength];
        byteBuf.readBytes(magicNum, 0, magicNumLength);
        for (int i = 0; i < magicNumLength; i++) {
            if (magicNum[i] != RpcConstant.MAGIC_NUM[i]) {
                throw new IllegalArgumentException("unknown0 magic number: " + Arrays.toString(magicNum));
            }
        }
        // 1字节 版本号，判断是否符合要求
        byte version = byteBuf.readByte();
        if (version != RpcConstant.VERSION) {
            throw new IllegalArgumentException("the version number does not match: " + version);
        }
        // 1字节 序列化类型
        byte serializerType = byteBuf.readByte();
        // 1字节 消息类型
        byte messageType = byteBuf.readByte();
        // 1字节 消息状态
        byte messageStatus = byteBuf.readByte();
        // 4字节 消息序列号ID
        int sequenceId = byteBuf.readInt();
        // 4字节 消息体长度
        int bodyLength = byteBuf.readInt();
        // 最后 消息体（正文）
        byte[] bytes = new byte[bodyLength];
        byteBuf.readBytes(bytes, 0, bodyLength);

        // 构建消息头
        RpcMessageHeader rpcMessageHeader = RpcMessageHeader.builder()
                .magicNum(magicNum)
                .version(version)
                .serializerType(serializerType)
                .messageType(messageType)
                .messageStatus(messageStatus)
                .sequenceId(sequenceId)
                .length(bodyLength).build();

        // 根据 序列化类型 获取序列化对象
        SerializerType serializationType = SerializerType.getByType(serializerType);
        Serializer serializer = SerializerFactory.getSerializer(serializationType);

        // 创建信息类对象（先设置消息头），获取消息类型，根据消息类型，反序列化对应的类（再设置消息体）
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setRpcMessageHeader(rpcMessageHeader);

        MessageType msgType = MessageType.getByType(messageType);
        if (msgType == MessageType.REQUEST) {
            // 反序列化
            RpcRequest rpcRequest = serializer.deserialize(RpcRequest.class, bytes);
            rpcMessage.setRpcMessageBody(rpcRequest);
        } else if (msgType == MessageType.RESPONSE) {
            // 反序列化
            RpcResponse rpcResponse = serializer.deserialize(RpcResponse.class, bytes);
            rpcMessage.setRpcMessageBody(rpcResponse);
        } else if (msgType == MessageType.HEARTBEAT_REQUEST || msgType == MessageType.HEARTBEAT_RESPONSE) {
            // 心跳检测的request和response都是用的同一个类，且类对象只有一个String属性，反序列化为String即可
            String message = serializer.deserialize(String.class, bytes);
            rpcMessage.setRpcMessageBody(message);
        }

        // 传递下一个handler
        list.add(rpcMessage);
    }
}

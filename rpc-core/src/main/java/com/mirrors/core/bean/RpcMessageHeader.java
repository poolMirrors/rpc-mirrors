package com.mirrors.core.bean;

import com.mirrors.core.constants.RpcConstant;
import com.mirrors.core.enums.MessageStatus;
import com.mirrors.core.enums.MessageType;
import com.mirrors.core.enums.SerializerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 网络传输的信息类--消息头（自定义协议）
 *
 * @author mirrors
 * @version 1.0
 * @date 2023/12/11 15:33
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RpcMessageHeader {
    /**
     * 4字节 魔数
     */
    private byte[] magicNum;

    /**
     * 1字节 版本号
     */
    private byte version;

    /**
     * 1字节 序列化算法
     */
    private byte serializerType;

    /**
     * 1字节 消息类型
     */
    private byte messageType;

    /**
     * 1字节 消息状态类型
     */
    private byte messageStatus;

    /**
     * 4字节 消息的序列号ID
     */
    private int sequenceId;

    /**
     * 4字节 数据内容长度
     */
    private int length;

    /**
     * 根据传进的序列化名字，选择不同的序列化算法进行创建 消息头RpcMessageHeader
     *
     * @param serializerName
     * @return
     */
    public static RpcMessageHeader createBySerializer(String serializerName) {
        return RpcMessageHeader.builder()
                .magicNum(RpcConstant.MAGIC_NUM) // 魔数
                .version(RpcConstant.VERSION) // 版本
                .serializerType(SerializerType.getByName(serializerName).getType()) // 序列化算法
                .messageType(MessageType.REQUEST.getType()) // 消息类型
                .sequenceId(RpcConstant.getSequenceId()).build(); // 消息序列id
    }

}

package com.example.emos.wx.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Collection;

/**
 * 记录收件人和相关已读状态
 */
@Data
@Document(collection = "message_ref") // 文档注解
public class MessageRef implements Serializable {

    @Id
    private String _id;

    @Indexed
    private String messageId;   // 应该对应 MessageEntity 的id？？

    @Indexed
    private Object receiverId;  // 可以是很多人 ？

    @Indexed
    private Boolean readFlag;   // 已读状态为什么是1个

    @Indexed
    private Boolean lastFlag;   // 是否为新接收的消息


}

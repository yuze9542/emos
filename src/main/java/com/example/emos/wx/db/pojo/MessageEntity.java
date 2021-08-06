package com.example.emos.wx.db.pojo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.io.Serializable;
import java.util.Date;

@Data
// 对应数据库名称
@Document(collection = "message") //ORM 映射相关
public class MessageEntity implements Serializable {
    @Id
    private String _id; // 自动生成的主键值

    @Indexed(unique = true)
    private String uuid;    // 防止消息重复

    @Indexed
    private Integer senderId;   // 发送者Id

    private String senderPhoto = "https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=2338648617,1956288521&fm=26&gp=0.jpg";

    private String senderName;

    private String msg;

    @Indexed //索引  提升性能
    private Date sendTime;
}

package com.example.emos.wx.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Component
@Scope("prototype") // 异步执行
/**
 * 发送邮件
 */
public class EmailTask implements Serializable {
    @Autowired
    private JavaMailSender javaMailSender;
    @Value("${emos.email.system}")
    private String mailbox;

    @Async // 异步？
    public void sendAsync(SimpleMailMessage message) {
        message.setFrom(mailbox);
        javaMailSender.send(message);
    }
}
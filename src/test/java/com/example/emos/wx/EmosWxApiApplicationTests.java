package com.example.emos.wx;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRef;
import com.example.emos.wx.db.pojo.TbMeeting;
import com.example.emos.wx.service.MeetingService;
import com.example.emos.wx.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Date;

@SpringBootTest
public class EmosWxApiApplicationTests {

    @Autowired
    private MessageService messageService;

    @Autowired
    private MeetingService meetingService;

    @Test
    void contextLoads() {
        for (int i = 1; i <= 100; i++) {
            MessageEntity message = new MessageEntity();
            message.setUuid(IdUtil.simpleUUID());
            message.setSenderId(0); // 发送人id
            message.setSenderName("系统消息");
            message.setMsg("这是第" + i + "条测试消息");
            message.setSendTime(new Date());
            String id = messageService.insertMessage(message);
            MessageRef ref = new MessageRef();
            ref.setMessageId(id);   // 发送人id
            ArrayList<Integer> list = new ArrayList<>();
            ref.setReceiverId(14); //注意：这是接收人ID
            ref.setLastFlag(true);  // ？？
            ref.setReadFlag(false);
            messageService.insertRef(ref);
        }
    }


    @Test
    void createMeeting() {
        for (int i = 0; i < 10; i++) {
            TbMeeting meeting = new TbMeeting();
            meeting.setId((long) i);
            meeting.setUuid(IdUtil.simpleUUID());
            meeting.setTitle("测试会议" + i);
            meeting.setCreatorId(14L); //ROOT用户ID int->long类型
            meeting.setDate(DateUtil.today());
            meeting.setPlace("线上会议室");
            meeting.setStart("08:30");
            meeting.setEnd("10:30");
            meeting.setType((short) 1);
            meeting.setMembers("[15,14]");
            meeting.setDesc("会议研讨Emos项目上线测试");
            meeting.setInstanceId(IdUtil.simpleUUID());  //工作流
            meeting.setStatus((short) 3);
            meeting.setCreateTime(new Date());
            meetingService.insertMeeting(meeting);
        }
    }


    void createCheckIn(){

    }
}

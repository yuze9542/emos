package com.example.emos.wx.service;


import com.example.emos.wx.db.pojo.TbMeeting;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface MeetingService {
    public void insertMeeting(TbMeeting meeting);

    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap params);

    public HashMap searchMeetingById(int id);

    public int updateMeetingInfo(HashMap param);

    public int deleteMeetingInfo(int id);

    public ArrayList<HashMap> searchMeetingByManagerDept(HashMap param);

    public int approvalMeetingInfo(int flag, int id);

    public List<String> searchUserMeetingInMonth(HashMap map);
}

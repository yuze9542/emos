package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbMeeting;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Mapper
public interface TbMeetingDao {

    int insertMeeting(TbMeeting meeting);

    ArrayList<HashMap> searchMyMeetingListByPage(HashMap map);

    public boolean searchMeetingMembersInSameDept(String uuid);

    HashMap searchMeetingById(int id);

    ArrayList<HashMap> searchMeetingByManagerDept(HashMap map);

    ArrayList<HashMap> searchMeetingMembers(int id);

    int updateMeetingInstanceId(HashMap map);

    int updateMeetingInfo(HashMap map);

    int updateMeetingFailById(int id);

    int updateMeetingSuccessById(int id);

    int updateMeetingOver(int id);

    int deleteMeetingInfo(int id);

    List<String> searchUserMeetingInMonth(HashMap param);
}
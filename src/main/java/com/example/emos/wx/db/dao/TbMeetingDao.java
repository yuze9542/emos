package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbMeeting;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
//会议status 1是刚创建 2是审核未通过 3是审核通过但未开始 4是正在进行中 5是结束了 6是已删除
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
    //逻辑删除 不物理删除了
    int updateMeetingToDelete(int id);

    int deleteMeetingInfo(int id);

    List<String> searchUserMeetingInMonth(HashMap param);

    int updateMeetingToStarting(int id);
}
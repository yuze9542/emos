package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbCheckin;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbCheckinDao {
    Integer haveCheckIn(HashMap param);

    String getCheckIn(HashMap map);

    HashMap searchTodayCheckin(int userId);

    long searchCheckinDays(int userId);

    void insert(TbCheckin tbCheckin);

    HashMap searchCheckInAndOutByDate(HashMap param);


    ArrayList<HashMap> searchMonthCheckin(HashMap param);
}
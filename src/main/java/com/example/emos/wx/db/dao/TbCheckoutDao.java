package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbCheckout;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbCheckoutDao {

    Integer haveCheckOut(HashMap param);

    HashMap searchTodayCheckOut(int userId);

    long searchCheckOutDays(int userId);

    // 为什么是list
    ArrayList<HashMap> searchWeekCheckOut(HashMap param);

    void insert(TbCheckout checkout);

    // 检查日期内是否打过卡
    String getCheckOut(HashMap pmap);

    void deleteCheckOut(HashMap pmap);
}



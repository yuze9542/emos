package com.example.emos.wx.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;

public interface CheckinService {
    // 验证能否签到
    public String validCanCheckIn(int userId, String date);

    // 签到
    public void checkIn(HashMap param);

    // 下班签到
    public void checkOut(HashMap param) throws IOException;

    // 创建人脸模型 改成了 密码验证
    public void createFaceModel(int userId, String path, String code);

    // 选择当日 签到结果
    public HashMap searchTodayCheckin(int userId);

    // 选择当日 下班签到结果
    public HashMap searchTodayCheckOut(int userId);

    // 查找签到总天数
    public long searchCheckinDays(int userId);

    // 查找本周内签到情况
    public ArrayList<HashMap> searchWeekCheckin(HashMap param);

    // 查询月考勤
    public ArrayList<HashMap> searchMonthCheckin(HashMap param);


    // 补签申请
    public void recheckIn(HashMap param);
}

package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

/**
 * 补签： 时间 地点 用户id
 */
@Data
@ApiModel
public class ReplenishCheckInForm {

    private String address;  // 地点
    private String userId;  //签到id(md5)

    private String date;    //要打卡的日期
    private String time;   // [打卡时间]  系统可指定 yyyy-mm-dd hh-mm-ss
    private String reason;   // 原因
    private Integer inOrOut;    // 上午还是下午   0上午 1下午
}

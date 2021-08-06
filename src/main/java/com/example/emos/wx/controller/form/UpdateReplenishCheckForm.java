package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@Data
@ApiModel
public class UpdateReplenishCheckForm {
    // 更新补勤 需要传入 同意 不同意 和 待处理 1是同意 11 待处理 / 12 不同意？
    private Integer reCheckId;  // 待处理表id
    private Integer status; // 状态 1： 签到表也要为1 补签表也要为1  11 待处理异常状态？ 12 为不同意
}

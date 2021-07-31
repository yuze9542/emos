package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@ApiModel
@Data
public class ApprovalMeetingInfoForm {

    @NotNull
    private Integer flag;

    @NotNull
    private Integer id;

}

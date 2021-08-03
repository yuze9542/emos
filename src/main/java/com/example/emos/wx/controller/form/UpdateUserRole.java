package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel
public class UpdateUserRole {

    private  Integer id;

    private String ChangedUser;

    private String roles;

}

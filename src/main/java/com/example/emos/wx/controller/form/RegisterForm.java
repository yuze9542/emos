package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@Slf4j
@ApiModel
public class RegisterForm {

    @NotBlank(message = "注册信息不能为空")
    @Pattern(regexp = "^[0-9]{6}$", message = "注册码必须是6为数字")
    private String registerCode;

    @NotBlank(message = "微信临时授权不能为空")
    private String code;

    @NotBlank(message = "昵称不能为空")
    private String nickName;

    @NotBlank(message = "照片不能为空")
    private String photo;


}

package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 后端验证库   创建Form类  类声明要添加 @ApiModel 用在返回对象类上
 * validation库在做后端验证的时候，要求必须用封装类（Form类）来保存客户端提交的数据，
 * 然后在封装类中，我们可以定义验证的规则，validation会执行这些规则，帮我们验证客户端提交的数据。
 */
@ApiModel
@Data
public class LoginForm {

    @NotBlank(message = "临时授权不能为空")
    private String code;
}

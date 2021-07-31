package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
@ApiModel
public class TestSayHelloForm {
    @NotBlank //不能为空
    // @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,15}$") // 正则表达式
    @ApiModelProperty("姓名") //  好像是可有可无 锦上添花的
    private String name;
}

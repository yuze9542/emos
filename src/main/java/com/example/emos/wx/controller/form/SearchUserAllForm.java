package com.example.emos.wx.controller.form;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
@ApiModel
public class SearchUserAllForm {
    @NotNull
    private Integer page;
    @NotNull
    private Integer length;

}

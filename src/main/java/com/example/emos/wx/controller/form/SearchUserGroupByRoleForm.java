package com.example.emos.wx.controller.form;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class SearchUserGroupByRoleForm {
    @NotNull
    private Integer id;
}

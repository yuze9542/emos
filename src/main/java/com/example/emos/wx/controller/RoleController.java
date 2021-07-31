package com.example.emos.wx.controller;


import cn.hutool.json.JSONUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.controller.form.InsertRoleForm;
import com.example.emos.wx.controller.form.SearchRoleOwnPermissionForm;
import com.example.emos.wx.controller.form.UpdateRolePermissionsForm;
import com.example.emos.wx.db.pojo.TbRole;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.RoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;

@RestController
@RequestMapping("/role")
@Api("角色模块网络接口")
public class RoleController {

    @Autowired
    private RoleService roleService;

    @PostMapping("/searchRoleOwnPermission")
    @ApiOperation("角色查询权限列表")
    @RequiresPermissions(value = {"ROOT", "ROLE:SELECT"}, logical = Logical.OR)
    public R searchRoleOwnPermission(@Valid @RequestBody SearchRoleOwnPermissionForm form) {
        ArrayList<HashMap> list = roleService.searchRoleOwnPermission(form.getId());
        return R.ok().put("result", list);
    }


    @PostMapping("/insertRole")
    @ApiOperation("添加角色")
    @RequiresPermissions(value = {"ROOT", "ROLE:INSERT"}, logical = Logical.OR)
    public R insertRole(@Valid @RequestBody InsertRoleForm form) {
        if (!JSONUtil.isJsonArray(form.getPermissions())) {
            throw new EmosException("权限不是数组格式");
        }
        TbRole entity = new TbRole();
        entity.setRoleName(form.getRoleName());
        entity.setPermissions(form.getPermissions());
        roleService.insertRole(entity);
        return R.ok().put("result", "success");
    }

    @PostMapping("/updateRolePermissions")
    @ApiOperation("修改角色")
    @RequiresPermissions(value = {"ROOT", "ROLE:UPDATE"}, logical = Logical.OR)
    public R updateRolePermissions(@Valid @RequestBody UpdateRolePermissionsForm form) {
        if (!JSONUtil.isJsonArray(form.getPermissions())) {
            throw new EmosException("权限不是数组格式");
        }
        TbRole entity = new TbRole();
        entity.setId(form.getId());
        entity.setPermissions(form.getPermissions());
        roleService.updateRolePermissions(entity);
        return R.ok().put("result", "success");
    }
}

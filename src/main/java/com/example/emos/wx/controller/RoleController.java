package com.example.emos.wx.controller;


import cn.hutool.json.JSONUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.controller.form.*;
import com.example.emos.wx.db.pojo.TbRole;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.RoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
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

    @PostMapping("/searchRole")
    @ApiOperation("角色查询列表")
    @RequiresPermissions(value = {"ROOT", "ROLE:SELECT"}, logical = Logical.OR)
    public R searchRole(@Valid @RequestBody SearchAllRoleForm form) {
        ArrayList<HashMap> list = roleService.searchAllRole(form.getPage(),form.getLength());
        return R.ok().put("result", list);
    }


    @GetMapping("/searchAllPermission")
    @ApiOperation("查询权限列表")
    @RequiresPermissions(value = {"ROOT", "ROLE:SELECT"}, logical = Logical.OR)
    public R searchAllPermission(@Valid @RequestParam Integer id) {
        ArrayList<HashMap> list = roleService.searchAllPermission(id);
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
        entity.setPermissions(form.getPermissions()); // 先把权限拿到
        roleService.updateRolePermissions(entity);
        return R.ok().put("result", "success");
    }

    @PostMapping("/deleteRole")
    @ApiOperation("删除角色")
    @RequiresPermissions(value = {"ROOT", "ROLE:INSERT"}, logical = Logical.OR)
    public R deleteRole(@Valid @RequestBody DeleteRoleForm form) {
        Integer id = form.getId();
        roleService.deleteRoleById(id);
        return R.ok();
    }


}

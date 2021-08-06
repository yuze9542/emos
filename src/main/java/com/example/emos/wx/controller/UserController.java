package com.example.emos.wx.controller;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.alibaba.druid.support.json.JSONUtils;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.*;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Api("用户模块")
public class UserController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;

    @PostMapping("/register")
    @ApiOperation("注册用户")
    public R register(@Valid @RequestBody RegisterForm form) {
        int id = userService.registerUser(form.getRegisterCode(), form.getCode(),
                form.getNickName(), form.getPhoto());
        String token = jwtUtil.createToken(id);
        Set<String> permsSet = userService.searchUserPermissions(id);
        saveCacheToken(token, id);
        return R.ok("用户注册成功").put("token", token).put("permission", permsSet);
    }


    @PostMapping("/login")
    @ApiOperation("登录系统")
    public R login(@Valid @RequestBody LoginForm form) {
        int id = userService.login(form.getCode());
        String token = jwtUtil.createToken(id); // 每次登陆id不一样
        Set<String> permsSet = userService.searchUserPermissions(id);
//        saveCacheToken(token,id);
        return R.ok("登陆成功").put("token", token).put("permission", permsSet);
    }

    private void saveCacheToken(String token, int userId) {
        redisTemplate.opsForValue().set(token, userId + "", cacheExpire, TimeUnit.DAYS);
    }

    @GetMapping("/searchUserSummary")
    @ApiOperation("查询用户摘要信息")
    public R searchUserSummary(@RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        HashMap map = userService.searchUserSummary(userId);
        map.put("id", userId);
        return R.ok().put("result", map);
    }

    @PostMapping("/searchUserGroupByDept")
    @ApiOperation("查询员工列表 按照部门分组排序")
    @RequiresPermissions(value = {"ROOT", "EMPLOYEE:SELECT"}, logical = Logical.OR)
    public R searchUserGroupByDept(@Valid @RequestBody SearchUserGroupByDeptForm form) {
        ArrayList<HashMap> list = userService.searchUserGroupByDept(form.getKeyword());
        return R.ok().put("list", list);
    }


    @PostMapping("/searchUserAll")
    @ApiOperation("查询员工列表 按照部门分组排序")
    @RequiresPermissions(value = {"ROOT", "EMPLOYEE:SELECT"}, logical = Logical.OR)
    public R searchUserAll(@Valid @RequestBody SearchUserAllForm form) {
        Integer length = form.getLength();
        Integer page = form.getPage();
        ArrayList<HashMap> list = userService.searchUserAll(page,length);
        return R.ok().put("result", list);
    }


    @PostMapping("/searchMembers")
    @ApiOperation("查询成员")
    @RequiresPermissions(value = {"ROOT", "MEETING:INSERT", "MEETING:UPDATE"}, logical = Logical.OR)
    public R searchMembers(@Valid @RequestBody SearchMembersForm form) {

        if (!JSONUtil.isJsonArray(form.getMembers())) {
            throw new EmosException("members 不是数组");
        }

        List<Integer> param = JSONUtil.parseArray(form.getMembers()).toList(Integer.class);
        ArrayList<HashMap> list = userService.searchMembers(param);
        return R.ok().put("result", list);
    }



    @PostMapping("/searchUserGroupByRole")
    @ApiOperation("根据角色表查询用户")
    @RequiresPermissions(value = {"ROOT", "ROLE:SELECT"}, logical = Logical.OR)
    public R searchUserGroupByRole(@Valid @RequestBody SearchUserGroupByRoleForm form) {
        Integer id = form.getId();
        ArrayList<HashMap> list = userService.searchUserGroupByRole(id);
        return R.ok().put("result", list);
    }

    /**
     * TODO 测试任务 测试多线程
     * @param form
     * @return
     */
    @PostMapping("/saveRoleByUserId")
    @ApiOperation("保存用户角色")
    @RequiresPermissions(value = {"ROOT", "ROLE:INSERT","ROLE:UPDATE"}, logical = Logical.OR)
    public R saveRoleByUserId(@Valid @RequestBody UpdateUserRole form) {
        Integer RoleId = form.getId();
        String roleUserSelected = form.getRoles();
        roleUserSelected = roleUserSelected.replace("&quot;", "\"");
        Map<String,Boolean> parse = (Map<String, Boolean>) JSONUtils.parse(roleUserSelected);

        List<Integer> changedUser = null;
        if (JSONUtil.isJsonArray(form.getChangedUser())){
            changedUser = JSONUtil.parseArray(form.getChangedUser()).toList(Integer.class);
        }
        userService.saveRoleByUserId(RoleId,parse,changedUser);


        return R.ok();
    }
}

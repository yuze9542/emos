package com.example.emos.wx.controller;

import com.example.emos.wx.common.util.R;
import com.example.emos.wx.controller.form.TestSayHelloForm;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/test")
@Api("测试web接口") //swagger用的 外
public class TestController {

    @PostMapping("/sayhello")
    @ApiOperation("最简单的测试方法") // 里
    // TestSayHelloForm 是 Valid库要求这么写的
    public R sayHello(@Valid @RequestBody TestSayHelloForm form) {
//        http://127.0.0.1:8080/emos-wx-api/swagger-ui.html#/
        return R.ok().put("message", "Hello," + form.getName());
    }

    @PostMapping("/addUser")
    @ApiOperation("添加用户")
    @RequiresPermissions(value = {"root", "USER:ADD"}, logical = Logical.OR)
    public R testShiro() {
        return R.ok("用户添加成功");
    }

}

package com.example.emos.wx.controller;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.*;
import com.example.emos.wx.db.pojo.TbMeeting;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MeetingService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/meeting")
@Api("会议模块")
@Slf4j
public class MeetingController {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private MeetingService meetingService;

    @PostMapping("/searchMyMeetingListPage")
    @ApiOperation("查询会议列表")
    public R searchMyMeetingListPage(@Valid @RequestBody SearchMyMeetingListByPageForm form,
                                     @RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        int page = form.getPage();
        int length = form.getLength();
        long start = (page - 1) * length;
        HashMap map = new HashMap();
        map.put("userId", userId);
        map.put("length", length);
        map.put("start", start);
        ArrayList<HashMap> list = meetingService.searchMyMeetingListByPage(map);

        return R.ok().put("result", list);
    }

    @PostMapping("/insertMeeting")
    @ApiOperation("添加会议")
    @RequiresPermissions(value = {"ROOT", "MEETING:INSERT"}, logical = Logical.OR)
    public R insertMeeting(@Valid @RequestBody InsertMeetingForm form, @RequestHeader("token") String token) {
        if (form.getType() == 2 && (form.getPlace() == null || form.getPlace().length() == 0)) {
            // 线下会议 有问题
            throw new EmosException("线下会议地点为空");
        }
        DateTime d1 = DateUtil.parse(form.getDate() + " " + form.getStart() + ":00");
        DateTime d2 = DateUtil.parse(form.getDate() + " " + form.getEnd() + ":00");
        if (d2.isBeforeOrEquals(d1)) {
            throw new EmosException("结束时间必须大于开始时间");
        }
        if (!JSONUtil.isJsonArray(form.getMembers())) {
            throw new EmosException("members必须是数组");
        }
        TbMeeting entity = new TbMeeting();
        entity.setUuid(UUID.randomUUID().toString(true));
        entity.setTitle(form.getTitle());
        entity.setCreatorId((long) jwtUtil.getUserId(token));
        entity.setDate(form.getDate());
        entity.setPlace(form.getPlace());
        entity.setStart(form.getStart() + ":00");
        entity.setEnd(form.getEnd() + ":00");
        entity.setType((short) form.getType());
        entity.setMembers(form.getMembers());
        entity.setDesc(form.getDesc());
        entity.setStatus((short) 1);
        meetingService.insertMeeting(entity);
        return R.ok().put("result", "success");
    }

    @PostMapping("/searchMeetingById")
    @ApiOperation("根据id查询会议")
    @RequiresPermissions(value = {"ROOT", "MEETING:SELECT"}, logical = Logical.OR)
    public R searchMeetingById(@Valid @RequestBody SearchMeetingByIdForm form) {
        HashMap map = meetingService.searchMeetingById(form.getId());
        return R.ok().put("result", map);
    }

    @PostMapping("/updateMeeting")
    @ApiOperation("更新会议")
    @RequiresPermissions(value = {"ROOT", "MEETING:UPDATE"}, logical = Logical.OR)
    public R updateMeeting(@Valid @RequestBody UpdateMeetingInfoForm form,
                           @RequestHeader("token") String token) {
        if (form.getType() == 2 && (form.getPlace() == null || form.getPlace().length() == 0)) {
            // 线下会议 有问题
            throw new EmosException("线下会议地点为空");
        }
        DateTime d1 = DateUtil.parse(form.getDate() + " " + form.getStart() + ":00");
        DateTime d2 = DateUtil.parse(form.getDate() + " " + form.getEnd() + ":00");
        if (d2.isBeforeOrEquals(d1)) {
            throw new EmosException("结束时间必须大于开始时间");
        }
        if (!JSONUtil.isJsonArray(form.getMembers())) {
            throw new EmosException("members必须是数组");
        }
        HashMap map = new HashMap();
        map.put("title", form.getTitle());
        map.put("date", form.getDate());
        map.put("place", form.getPlace());
        map.put("start", form.getStart());
        map.put("end", form.getEnd());
        map.put("type", form.getType());
        map.put("members", form.getMembers());
        map.put("desc", form.getDesc());
        map.put("id", form.getId());
        map.put("instanceId", form.getInstanceId());
        map.put("status", 3);
        meetingService.updateMeetingInfo(map);
        return R.ok().put("result", "success");
    }


    @PostMapping("/deleteMeetingById")
    @ApiOperation("删除会议")
    @RequiresPermissions(value = {"ROOT", "MEETING:DELETE"}, logical = Logical.OR)
    public R deleteMeetingById(@Valid @RequestBody DeleteMeetingInfoForm form) {
        meetingService.deleteMeetingInfo(form.getId());
        return R.ok().put("result", "success");
    }

    @PostMapping("/searchMeetingByManagerDept")
    @ApiOperation("查询未审批的会议")
    @RequiresPermissions(value = {"ROOT", "MEETING:SELECT"}, logical = Logical.OR)
    public R searchMeetingByManagerDept(@Valid @RequestBody SearchMeetingByManagerDeptForm form
            , @RequestHeader("token") String token) {
        HashMap param = new HashMap();
        int userId = jwtUtil.getUserId(token);
        int page = form.getPage();
        int length = form.getLength();
        long start = (page - 1) * length;
        param.put("id", userId);
        param.put("start", start);
        param.put("length", length);
        ArrayList<HashMap> list = meetingService.searchMeetingByManagerDept(param);
        return R.ok().put("result", list);
    }


    @PostMapping("/approvalMeeting")
    @ApiOperation("审批未审批的会议")
    @RequiresPermissions(value = {"ROOT", "MEETING:UPDATE"}, logical = Logical.OR)
    public R approvalMeeting(@Valid @RequestBody ApprovalMeetingInfoForm form
            , @RequestHeader("token") String token) {
        int i = meetingService.approvalMeetingInfo(form.getFlag(), form.getId());
        if (i != 1)
            throw new EmosException("出问题了");
        return R.ok().put("result", "success");
    }

    @PostMapping("/searchUserMeetingInMonth")
    @ApiOperation("查询月会议")
    public R searchUserMeetingInMonth(@Valid @RequestBody SearchUserMeetingInMonthForm form
            , @RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        HashMap map = new HashMap();
        map.put("userId", userId);
        map.put("express", form.getYear() + "/" + form.getMonth());
        List<String> list = meetingService.searchUserMeetingInMonth(map);
        return R.ok().put("result", list);
    }

}

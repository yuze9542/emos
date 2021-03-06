package com.example.emos.wx.controller;


import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.controller.form.*;
import com.example.emos.wx.db.SystemConstants;
import com.example.emos.wx.db.pojo.TbRecheck;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.service.UserService;
import com.example.emos.wx.utils.TypeChange;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


@RequestMapping("/checkin")
@RestController
@Api("签到模块Web接口")
@Slf4j
/**
 * 本类实现以下功能
 *  上班签到
 *  下班签到
 *  创建第一次签到模型
 *  查看用户今天是否可以签到
 */
public class CheckinController {


    private final String  ADMIN_DEPARTMENT_NAME = "行政部";

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CheckinService checkinService;


    @Autowired
    private UserService userService;

    @Autowired
    private SystemConstants systemConstants;

    @Value("${emos.image-folder}")
    private String imageFolder;

    @PostMapping("/checkin")
    @ApiOperation("上班签到功能")
    public R CheckIn(@Valid CheckInForm checkInForm,
                     @RequestParam("photo") MultipartFile file,
                     @RequestHeader("token") String token) {
        if (file == null) {
            return R.error("没有上传文件");
        }
        int userId = jwtUtil.getUserId(token);
        String fileName = file.getOriginalFilename().toLowerCase(); // 得到照片名字
        if (!fileName.endsWith(".jpg")) {
            return R.error("要jpg格式照片");
        } else {
            String path = imageFolder + "/" + fileName;
            try {
                file.transferTo(Paths.get(path)); // 可以改名
                HashMap param = new HashMap();
                param.put("userId", userId);
                param.put("file", file);
                param.put("path", path);
                param.put("city", checkInForm.getCity());
                param.put("district", checkInForm.getDistrict());
                param.put("address", checkInForm.getAddress());
                param.put("country", checkInForm.getCountry());
                param.put("province", checkInForm.getProvince());
                param.put("province", checkInForm.getProvince());
                param.put("userCheckInId", checkInForm.getUserCheckInId()); // 保存的是md5
                checkinService.checkIn(param);
                return R.ok("签到成功");
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new EmosException("图片保存错误");
            } finally {
                FileUtil.del(path);
            }

        }


    }


    @PostMapping("/checkout")
    @ApiOperation("下班签到")
    // 传过来的userId 经过了md5 加密
    public R checkout(@Valid CheckOutForm checkOutForm,
                      @RequestParam("photo") MultipartFile file,
                      @RequestHeader("token") String token) {
        if (file == null) {
            return R.error("没有上传文件");
        }
        int userId = jwtUtil.getUserId(token);
        String fileName = file.getOriginalFilename().toLowerCase(); // 得到照片名字
        if (!fileName.endsWith(".jpg")) {
            return R.error("要jpg格式照片");
        } else {
            String path = imageFolder + "/" + fileName;
            try {
                file.transferTo(Paths.get(path)); // 可以改名
                HashMap param = new HashMap();
                param.put("userId", userId);
                param.put("file", file);
                param.put("path", path);
                param.put("city", checkOutForm.getCity());
                param.put("district", checkOutForm.getDistrict());
                param.put("address", checkOutForm.getAddress());
                param.put("country", checkOutForm.getCountry());
                param.put("province", checkOutForm.getProvince());
                param.put("province", checkOutForm.getProvince());
                param.put("userCheckOutId", checkOutForm.getUserCheckOutId()); // 保存的是md5
                checkinService.checkOut(param);
                return R.ok("签到成功");
            } catch (IOException e) {
                log.error(e.getMessage(), e);
                throw new EmosException("图片保存错误");
            } finally {
                FileUtil.del(path);
            }

        }


    }


    /**
     * 第一次 创建人脸模型
     *  FIXME 创建到MongoDB
     * @param userCheckInId
     * @param file
     * @param token
     * @return
     */
    @PostMapping("/createFaceModel")
    @ApiOperation("创建人脸模型")
    public R CreateFaceModel(@Valid CreateUserCodeForm userCheckInId,
                             @RequestParam("photo") MultipartFile file,
                             @RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        if (userCheckInId.getUserCheckInId().equals("d41d8cd98f00b204e9800998ecf8427e")) {
            return R.error("验证码不能为空");
        }
        String userTempCheckId = userCheckInId.getUserCheckInId();
        if (file == null) {
            return R.error("没有上传文件");
        }
        String fileName = file.getOriginalFilename().toLowerCase();
        String path = imageFolder + "/" + userId + "_origin_create.jpg";
        if (!fileName.endsWith(".jpg")) {
            return R.error("必须提交JPG格式图片");
        } else {
            try {
                file.transferTo(Paths.get(path));
                checkinService.createFaceModel(userId, path, userTempCheckId);
                return R.ok("人脸建模成功");
            } catch (IOException e) {
                log.error(e.getMessage());
                throw new EmosException("保存图片错误");
            } finally {
//                FileUtil.del(path);
            }
        }
    }


    @GetMapping("/validCanCheckIn")
    @ApiOperation("查看用户今天是否可以签到")
    public R validCanCheckIn(@RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        String result = checkinService.validCanCheckIn(userId, DateUtil.today());
        return R.ok(result);
    }

    @GetMapping("/searchTodayCheckin")
    @ApiOperation("查询用户当日签到数据")
    public R searchTodayCheckin(@RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        boolean onlyCheckInNoCheckOut = true; // 只上班打卡没下班打开
//        选择当日 签到结果
        HashMap map = checkinService.searchTodayCheckin(userId);
        HashMap endMap = checkinService.searchTodayCheckOut(userId);
        if (endMap.get("status") != null && endMap.get("workTime")!=null) {
            onlyCheckInNoCheckOut = false;
            // 计算工作时间
            int t = (int) endMap.get("workTime");
            String h = Integer.toString(t / 60);
            String m = Integer.toString(t % 60);
            String workTime = h + "时" + m + "分";
            map.put("workTime", workTime);
            String endStatus = endMap.get("status").toString();
            String checkoutTime = endMap.get("checkoutTime").toString();
            map.put("endStatus", endStatus);
            map.put("checkoutTime", checkoutTime);
            onlyCheckInNoCheckOut = false;
        }

        map.put("attendanceTime", systemConstants.attendanceTime);
        map.put("closingTime", systemConstants.closingTime);
        // 查找签到总天数
        long days = checkinService.searchCheckinDays(userId);
        map.put("checkinDays", days);
        //判断日期是否在用户入职之前
        // 入职日期 DateUtil.parse把字符串转化为日期对象
        DateTime hiredate = DateUtil.parse(userService.searchUserHireDate(userId));
        // 本周开始日期
        DateTime startDate = DateUtil.beginOfWeek(DateUtil.date());
        // 本周开始日期在上班日期之前 即周三入职 周一周二不打卡
        if (startDate.isBefore(hiredate)) {
            startDate = hiredate;
        }
        // 结束日期
        DateTime endDate = DateUtil.endOfWeek(DateUtil.date());
        HashMap param = new HashMap();
        param.put("startDate", startDate.toString());
        param.put("endDate", endDate.toString());
        param.put("userId", userId);
        ArrayList<HashMap> list = null;
        if (!onlyCheckInNoCheckOut) {
            list = checkinService.searchWeekCheckin(param);
            map.put("weekCheckin", list);
        }
        // map是给前端的 param给后端
        return R.ok().put("result", map);
    }

    @PostMapping("/searchMonthCheckin")
    @ApiOperation("查询用户某月签到数据")
    public R searchMonthCheckin(@Valid @RequestBody SearchMonthCheckinForm form, @RequestHeader("token") String token) {
        int userId = jwtUtil.getUserId(token);
        //查询入职日期
        DateTime hiredate = DateUtil.parse(userService.searchUserHireDate(userId));
        //把月份处理成双数字
        String month = form.getMonth() < 10 ? "0" + form.getMonth() : "" + form.getMonth();
        //某年某月的起始日期
        DateTime startDate = DateUtil.parse(form.getYear() + "-" + month + "-01");
        //如果查询的月份早于员工入职日期的月份就抛出异常
        if (startDate.isBefore(DateUtil.beginOfMonth(hiredate))) {
            throw new EmosException("只能查询考勤之后日期的数据");
        }
        //如果查询月份与入职月份恰好是同月，本月考勤查询开始日期设置成入职日期
        if (startDate.isBefore(hiredate)) {
            startDate = hiredate;
        }
        //某年某月的截止日期
        DateTime endDate = DateUtil.endOfMonth(startDate);
        HashMap param = new HashMap();
        param.put("startDate", startDate.toString());
        param.put("endDate", endDate.toString());
        param.put("userId", userId);
        ArrayList<HashMap> list = checkinService.searchMonthCheckin(param);
        //统计月考勤数据
        int sum_1 = 0, sum_2 = 0, sum_3 = 0;
        for (HashMap<String, String> map : list) {
            String type = map.get("type");
            String status = map.get("status");
            if ("工作日".equals(type)) {
                if ("正常".equals(status)) {
                    sum_1++;
                } else if ("迟到".equals(status)) {
                    sum_2++;
                } else if ("缺勤".equals(status)) {
                    sum_3++;
                }
            }
        }
        return R.ok().put("list", list).put("sum_1", sum_1).put("sum_2", sum_2).put("sum_3", sum_3);
    }

    @PostMapping("/replenishCheckIn")
    @ApiOperation("补勤")
    public R reInsertCheckIn(@Valid @RequestBody ReplenishCheckInForm form,
                             @RequestHeader("token") String token) {

        // 需要验证 新加功能 验证 无限级别验证
        int userId = jwtUtil.getUserId(token);  // 后端的 userId
        String md5UserId = form.getUserId();    // 前端的 userId
        String md5Str = DigestUtils.md5DigestAsHex(Integer.toString(userId).getBytes());
        if (!(md5Str).equals(md5UserId)) throw new EmosException("口令输入错误");

        // 1 获取创建人的部门 以及部门负责人id和人事部同事们的id
        ArrayList<String> departments = new ArrayList<>();
        departments.add(ADMIN_DEPARTMENT_NAME);
        List<Integer> approvalList = userService.searchRelatedIdsByUserId(userId, departments);
        approvalList.add(userId); // FIXME 审批审批人 加自己 记得删除
        String approvalIds = TypeChange.ListToString(approvalList); // 列表转字符串

        DateTime  checkTableTIme = DateUtil.parse(form.getDate()+" "+form.getTime());  // form.getTime() 是签到表上应该写的的时间
        int inOrOut = form.getInOrOut();    // 打卡上午还是下午 int4个字节  byte1个字节

        TbRecheck recheck = new TbRecheck();    // 放入TbRecheck 传入service层
        recheck.setUserId(userId);
        recheck.setApproverId(approvalIds);
        recheck.setAddress(form.getAddress());
        recheck.setInorout((byte)inOrOut);
        recheck.setStatus(11);  // 前后端统一 待确认状态为11 和 12 都是上下班的待确认 默认11 12可以是不同意
        recheck.setReason(form.getReason()); //原因
        recheck.setTime(checkTableTIme);    // 签到表的创建时间
        recheck.setDate(form.getDate());
        recheck.setCreateTime(new DateTime());
        checkinService.insertReCheck(recheck);

        return R.ok();
    }


    @PostMapping("/UpdateReplenishCheck")
    @ApiOperation("补勤审批")
    public R UpdateReplenishCheck(@Valid @RequestBody UpdateReplenishCheckForm form,
                             @RequestHeader("token") String token) {

        int userId = jwtUtil.getUserId(token);
        checkinService.updateReCheckStatus(userId,form.getReCheckId(),form.getStatus());


        return R.ok();
    }

    @PostMapping("/searchReCheckListApproverIsMe")
    @ApiOperation("寻找补签表中 审批人包含自己的")
    public R searchReCheckListById(
            @Valid @RequestBody SelectReplenishYetCheckForm form,
            @RequestHeader("token") String token) {

        int userId = jwtUtil.getUserId(token);
        List<HashMap> list = userService.searchReCheckListById(userId);

        return R.ok().put("result",list);
    }

    @PostMapping("/searchReCheckById")
    @ApiOperation("寻找补签表")
    public R searchReCheckById(
            @Valid @RequestBody SearchReCheckByIdForm form,
            @RequestHeader("token") String token) {

        int userId = jwtUtil.getUserId(token);
        Integer id = form.getId();
        HashMap list = checkinService.searchReCheckById(id);

        return R.ok().put("result",list);
    }

}
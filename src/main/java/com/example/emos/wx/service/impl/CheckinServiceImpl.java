package com.example.emos.wx.service.impl;

import cn.hutool.core.date.*;
import com.example.emos.wx.common.util.FaceCompareUtils;
import com.example.emos.wx.common.util.File2Base64;
import com.example.emos.wx.config.shiro.JwtUtil;
import com.example.emos.wx.db.SystemConstants;
import com.example.emos.wx.db.dao.*;
import com.example.emos.wx.db.pojo.TbCheckin;
import com.example.emos.wx.db.pojo.TbCheckout;
import com.example.emos.wx.db.pojo.TbFaceModel;
import com.example.emos.wx.db.pojo.TbRecheck;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.CheckinService;
import com.example.emos.wx.db.dao.TbCheckoutDao;
import com.example.emos.wx.utils.TypeChange;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bytecode.Throw;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@Slf4j
public class CheckinServiceImpl implements CheckinService {



    @Autowired
    private TbFaceModelDao faceModelDao;

    @Autowired
    private SystemConstants systemConstants;

    @Autowired
    private TbCheckinDao checkinDao;

    @Autowired
    private TbCheckoutDao checkoutDao;

    @Autowired
    private TbUserDao userDao;

    @Autowired
    private TbHolidaysDao holidaysDao;

    @Autowired
    private TbWorkdayDao workdayDao;

    @Autowired
    private TbRecheckDao recheckDao;


    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FaceCompareUtils faceCompareUtils;



    /**
     * 返回 签到返回的消息
     */
    @Override
    public String validCanCheckIn(int userId, String date) {

        boolean isHolidayButNotWorkday = holidaysDao.searchTodayIsHolidays() != null ? true : false;
        boolean isWorkdayButNotHoliday = workdayDao.searchTodayIsWorkday() != null ? true : false;
        String type = "工作日";
        if (DateUtil.date().dayOfWeek() == 1) {
            type = "休息日";
        }
        if (isHolidayButNotWorkday) {
            type = "休息日";
        } else if (isWorkdayButNotHoliday) {
            type = "工作日";
        }
        if (type.equals("休息日"))
            return "节假日不需要签到";
        // 当前时间
        DateTime now = DateUtil.date();
        // 本日打卡开始
        String start = DateUtil.today() + " " + systemConstants.attendanceStartTime;
        // 本日结束打卡
        String end = DateUtil.today() + " " + systemConstants.attendanceEndTime;
        // 本日下班打卡开始
        String closingStart = DateUtil.today() + " " + systemConstants.closingStartTime;
        // 本日下班结束打卡
//        2021-04-07 16:00 2021-04-07 23:59
        DateTime attendanceStart = DateUtil.parse(start); //
        DateTime attendanceEnd = DateUtil.parse(end);
        DateTime closingStartTime = DateUtil.parse(closingStart);

        // 最晚下班打卡时间是今天的三点
        DateTime lastClosingEndTime = DateUtil.parse(DateUtil.today() + " " + systemConstants.closingEndTime);
        DateTime midnightTime = DateUtil.parse(DateUtil.today() + " 00:00");

        // 凌晨三点之前 负责前一天的下班打卡
        if (now.isBefore(lastClosingEndTime) && now.isAfter(midnightTime)){
            // 先查询上班打卡了没
            HashMap map = new HashMap();
            map.put("userId", userId);
            String yesterday = DateUtil.yesterday().toDateStr();
            map.put("date", yesterday);  //2021-07-31 当天 这里应该设置前一天
            start = yesterday + " " + systemConstants.attendanceStartTime;
            map.put("start", start);
            end = yesterday + " " + systemConstants.attendanceEndTime;
            map.put("end", end);
            boolean yesterdayIsCheckIn = checkinDao.haveCheckIn(map) != null ? true : false;
            if (yesterdayIsCheckIn){
                HashMap yesterdayIsCheckOutMap = new HashMap();
                yesterdayIsCheckOutMap.put("userId", userId);
                yesterdayIsCheckOutMap.put("date", DateUtil.yesterday().toDateStr());  //2021-07-31 当天 这里应该设置前一天
                start = DateUtil.yesterday().toDateStr() + " " + systemConstants.closingStartTime;
                map.put("start", start);
                end = DateUtil.yesterday().toDateStr() + " 24:00";
                map.put("end", end);
                boolean yesterdayIsCheckOut = checkoutDao.haveCheckOut(map) != null ? true : false;
                // 如果查询不到下班打卡记录 就提示该下班了
                if (!yesterdayIsCheckOut)
                    return "可以下班考勤";
            }
            return "太早了 不睡觉啊你！";
        }

        // 如果当前时间在 上班打卡时间前 且 前一日最晚打卡时间之后
        else if (now.isBefore(attendanceStart) && now.isAfter(lastClosingEndTime)) {
            return "没开始上班呢";
        }
        else if (now.isAfter(attendanceEnd) && now.isBefore(closingStartTime)) {
            return "超过了上班考勤结束时间";
        }
        else {
            HashMap map = new HashMap();
            map.put("userId", userId);
            map.put("date", date);
            map.put("start", start);
            map.put("end", end);
            boolean b = checkinDao.haveCheckIn(map) != null ? true : false;
            if (b) { // 早上打过卡了

                map.put("start", DateUtil.today() + " " + systemConstants.closingStartTime);
                map.put("end", DateUtil.tomorrow() + " " + systemConstants.closingEndTime);
                // 判断下午打没打卡
                if (now.isBefore(closingStartTime)){
                    return "上午打卡完成，还没到下午打卡时间";
                }
                boolean c = checkoutDao.haveCheckOut(map) != null ? true : false;

                return c ? "下班考勤卡已打" : "可以下班考勤";
            }
            return "可以上班考勤";
        }
    }

    /**
     * 上班签到
     *
     * @param param
     */
    @Override
    public void checkIn(HashMap param) {
        // 当前时间d1 在 d2之前为正常 status为1
        Date d1 = DateUtil.date();
        // 上班时间
        Date d2 = DateUtil.parse(DateUtil.today() + " " + systemConstants.attendanceTime);
        // 上班打卡最晚时间
        Date d3 = DateUtil.parse(DateUtil.today() + " " + systemConstants.attendanceEndTime);
        // 下班打卡时间
        Date d4 = DateUtil.parse(DateUtil.today() + " " + systemConstants.closingStartTime);
        int status = 1; // 正常考勤
        // 当前时间 在 上班结束时间之前为正常
        if (d1.compareTo(d2) <= 0) {
            status = 1; // 正常
        }
        // 当前时间 在上班时间之后但在最后打卡时间d3之前 为迟到
        else if (d1.compareTo(d2) > 0 && d1.compareTo(d3) < 0) {
            status = 2; // 迟到
        }
        // 如果当前时间在最后打卡时间d3之后 为旷工
        else if (d1.compareTo(d3) > 0 && d1.compareTo(d4) < 0) {
            status = 3; //旷工
        }
        int userId = (Integer) param.get("userId");
        TbFaceModel faceModel = faceModelDao.selectByUserId(userId); // faceModel 改成了 验证id

        if (faceModel == null)
            throw new EmosException("不存在人脸模型"); // 创建人脸模型
        else {
            // 正常签到流程： 检查用户传过来的验证id 是否和之前的匹配
            // 若匹配 则成功 不匹配则失败

            // 2021-7-30 新增人脸识别功能
            String fromClientPhotoBase64 = File2Base64.MultipartFile2Base64((MultipartFile) param.get("file"));
            String originPhotoPath = faceModel.getFacePath();
            String fromServerPhoto = File2Base64.path2Base64(originPhotoPath);
            int compare = 0;
            try {
                compare = faceCompareUtils.compare(fromClientPhotoBase64, fromServerPhoto);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (compare<60){
                throw new EmosException("不是本人");
            }

            String userCHeckINId = (String) param.get("userCheckInId");
            String fromSqlUserId = faceModel.getUserId() + "";
            String md5Str = DigestUtils.md5DigestAsHex(fromSqlUserId.getBytes());
            if (userCHeckINId.equals(md5Str)) {
                // 正常
                // 风险等级
                int risk = 1;
                String city = (String) param.get("city"); // 城市
                String district = (String) param.get("district"); // 区域

                // 保存签到记录
                String address = (String) param.get("address");
                String country = (String) param.get("country");
                String province = (String) param.get("province");

                TbCheckin entity = new TbCheckin();
                entity.setUserId(userId);
                entity.setAddress(address);
                entity.setCountry(country);
                entity.setProvince(province);
                entity.setCity(city);
                entity.setDistrict(district);
                entity.setStatus((byte) status);
                entity.setRisk(risk);
                entity.setDate(DateUtil.today());
                entity.setCreateTime(d1);
                checkinDao.insert(entity);
            } else {
                throw new EmosException("输入的个人身份ID错误");
            }
        }
    }

    @Override
    /**
     * 下班签到
     */
    public void checkOut(HashMap param) throws IOException {

        // 先看看白天上班打卡了没
        String checkIn = _checkCheckInCreateTimeByDate((Integer) param.get("userId"),DateUtil.today()+"");

        // 上班打过卡了
        if (checkIn != null && checkIn != "") {

            // 2021-7-31 新增判断下班是不是打过卡了 打过要删除
            HashMap isCheckOutMap = new HashMap();   // 里面装的是上班日期 时间和userId
            isCheckOutMap.put("userId", (Integer) param.get("userId"));
            isCheckOutMap.put("date", DateUtil.today());
            HashMap pmap = new HashMap();   // 里面装的是上班日期 时间和userId
            pmap.put("userId", (Integer) param.get("userId"));
            pmap.put("date", DateUtil.today());
            String checkOut = checkoutDao.getCheckOut(pmap);
            if (checkOut!=null && checkOut!=""){
                // 存在下班打卡记录要删除
                checkoutDao.deleteCheckOut(pmap);
            }
            //打卡时间
            Date d1 = DateUtil.date();
            // 下班打卡开始时间
            Date d2 = DateUtil.parse(DateUtil.today() + " " + systemConstants.closingStartTime);
            Date d = DateUtil.parse(DateUtil.today() + " " + systemConstants.closingTime);
            // 下班打卡结束时间
            Date d3 = DateUtil.parse(DateUtil.tomorrow().toDateStr() + " " +  systemConstants.closingEndTime);
            int status = 1; // 正常考勤
            // 说明： 如果每日工作量超过八小时 属于正常 如果五点半下班前属于早退
            // 超过九个小时属于加班
            if (d1.compareTo(d2) < 0){
                 throw new EmosException("还未到打卡时间");
            }
            if (d1.compareTo(d)<0){
                status = 2; // 早退
            }
            // 打卡时间在 d2~d3时间
            else if (d1.compareTo(d2) > 0 && d1.compareTo(d3) < 0) {
                status = 1;
            }
            int userId = (Integer) param.get("userId");

            // 人脸识别
            TbFaceModel faceModel = faceModelDao.selectByUserId(userId); // faceModel 改成了 验证id
            if (faceModel == null)
                throw new EmosException("不存在人脸模型"); // 创建人脸模型
            else {


                // 2021-7-30 新增人脸识别功能
                String fromClientPhotoBase64 = File2Base64.MultipartFile2Base64((MultipartFile) param.get("file"));
                String originPhotoPath = faceModel.getFacePath();
                String fromServerPhoto = File2Base64.path2Base64(originPhotoPath);
                int compare = faceCompareUtils.compare(fromClientPhotoBase64, fromServerPhoto);
                if (compare<60){
                    throw new EmosException("不是本人");
                }
                String userCheckOutId = (String) param.get("userCheckOutId"); //签到的时候输入的
                String fromSqlUserId = faceModel.getUserId() + "";  // 后端获取的通过tokenId
                String md5Str = DigestUtils.md5DigestAsHex(fromSqlUserId.getBytes());
                // 正常签到流程： 检查用户传过来的验证id 是否和之前的匹配
                // 若匹配 则成功 不匹配则失败
                // userCHeckOutId 是传过来的
                if (userCheckOutId.equals(md5Str)) {
                    // 正常
                    // 风险等级
                    int risk = 1;
                    String city = (String) param.get("city"); // 城市
                    String district = (String) param.get("district"); // 区域
//                    checkIn = 31 11:28


                    Integer h1 = Integer.valueOf(checkIn.substring(11,13));
                    Integer m1 = Integer.valueOf(checkIn.substring(14,16));
                    String checkInDay = checkIn.substring(8, 10);

                    // 早上的时间
                    int t1 = h1 * 60 + m1;

                    // 下班签到的时间要判断是不是第二天
                    Calendar cal = Calendar.getInstance();

                    String nowDay = String.valueOf(cal.get(Calendar.DAY_OF_MONTH));
                    cal.get(Calendar.DAY_OF_MONTH);
                    Integer nowDate = Integer.parseInt(nowDay);
                    int h2 = cal.get(cal.HOUR_OF_DAY);
                    int m2 = cal.get(cal.MINUTE);
                    int t2 = h2 * 60 + m2 + 1;
                    Integer checkInDate = Integer.parseInt(checkInDay);
                    if (Integer.compare(checkInDate,nowDate)!=0){
                        // 说明第二天回的
                        // t2 还要加上前一天工作量 t2 = now + 24:00 - 上班时间
                        t2 += (24 * 60 - t1);
                    }
                    int workTime = t2 - t1;
                    if ( workTime < 8 * 60 && status == 1){
                        status = 3; // 正常下班打卡 但工作量不够 应该是迟到了
                    }else if (workTime < 8 * 60 && status == 2){
                        status = 4; // 早退 那肯定是
                    }
                    if (workTime>10 * 60) {
                        status = 1; // 工作量超了 迟不迟到无所谓
                    }
                    // 保存签到记录
                    String address = (String) param.get("address");
                    String country = (String) param.get("country");
                    String province = (String) param.get("province");

                    TbCheckout entity = new TbCheckout();
                    entity.setUserId(userId);
                    entity.setAddress(address);
                    entity.setCountry(country);
                    entity.setProvince(province);
                    entity.setCity(city);
                    entity.setDistrict(district);
                    entity.setStatus((byte) status);
                    entity.setRisk(risk);
                    if (h2<=3){ // 如果是凌晨三点以前下班打卡就是前一天
                        entity.setDate(DateUtil.yesterday().toDateStr());
                    }else {
                        entity.setDate(DateUtil.today());
                    }
                    entity.setCreateTime(d1);
                    entity.setWorkTime(workTime);
                    checkoutDao.insert(entity);
                } else {
                    throw new EmosException("输入的个人身份ID错误");
                }
            }
        }
    }


    /**
     *  保存的代码在 controller 里
     * @param userId
     * @param path
     */
    @Override
    public void createFaceModel(int userId, String path, String userCheckInId) {

        TbFaceModel entity = new TbFaceModel();
        entity.setUserId(userId);
        entity.setFacePath(path);// 本来传path 现在改成了openId
        faceModelDao.insert(entity);
    }

    /**
     * 选择当日 签到结果
     *
     * @param userId
     * @return
     */
    @Override
    public HashMap searchTodayCheckin(int userId) {
        return checkinDao.searchTodayCheckin(userId);
    }

    @Override
    public HashMap searchTodayCheckOut(int userId) {
        HashMap map = checkoutDao.searchTodayCheckOut(userId);
        return map;
    }

    /**
     * 查找签到总天数
     *
     * @param userId
     * @return
     */
    @Override
    public long searchCheckinDays(int userId) {
        return checkinDao.searchCheckinDays(userId);
    }

    @Override
    // 新方法 不用查询那么多次
    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
        // 用户本周考勤情况
        // 月查询查询一次数据库 周查询查询多次数据库 月查询如果查询30次 太浪费了
        ArrayList<HashMap> checkInList = checkinDao.searchMonthCheckin(param);
        //本周特殊的节假日
        ArrayList<String> holidaysList = holidaysDao.searchHolidaysInRange(param);
        // 本周特殊工作日
        ArrayList<String> workDayList = workdayDao.searchWorkDayInRange(param);
        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
        // 注意这里不能写parseDate 要写parse
        DateTime endDate = DateUtil.parse(param.get("endDate").toString());
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);
        ArrayList list = new ArrayList();
        range.forEach(one -> {
            String date = one.toString("yyyy-MM-dd");
            //查看今天是不是假期或者工作日
            String type = "工作日";

            if (one.isWeekend() ) {
                type = "节假日";
            }
            if (holidaysList != null && holidaysList.contains(date)) {
                type = "节假日";
            } else if (workDayList != null && workDayList.contains(date)) {
                type = "工作日";
            }
            String status = "";
            if (DateUtil.compare(one, DateUtil.date()) <= 0) {
                status = "缺勤";
                boolean flag = false;

                for (HashMap<String, Object> map : checkInList) {
                    if (date.equals(map.get("ciDate"))) {
                        // 此时 肯定有上班打卡状态 1为正常 2为迟到
                        // 但不一定有下班打卡时间和工作量

                        Integer checkInStatus = (Integer) map.get("checkInStatus");
                        Integer checkOutStatus = (Integer) map.get("checkOutStatus");
                        Integer workTime = (Integer) map.get("workTime");
                        // 上班签到状态
                        if (Integer.compare(checkInStatus, 1) != 0){
                            status = "迟到";
                        }
                        // 未下班签到
                        if (checkOutStatus==null || Integer.compare(checkOutStatus, 1) != 0){
                            status = "早退";
                        }else if (Integer.compare(checkOutStatus, 1) == 0){
                            status = "正常";
                            if (Integer.compare(workTime, 60* 10) > 0){
                                status = "加班";
                            }
                        }
                        flag = true;
                        break;
                    }
                    DateTime endTime = DateUtil.parse(DateUtil.tomorrow().toDateStr() + " " + systemConstants.attendanceEndTime);
                    String today = DateUtil.today();
                    if (date.equals(today) && DateUtil.date().isBefore(endTime) && flag == false) {
                        status = "";
                    }
                }
            }
            HashMap map = new HashMap();
            map.put("date", date);
            map.put("status", status);
            map.put("type", type);
            map.put("day", one.dayOfWeekEnum().toChinese("周"));
            list.add(map);
        });
        return list;
    }

    /**
     * 查找本周内签到情况 旧方法
     * step1 先查询出本周考勤记录 特殊工作日和节假日
     * step2 用DateUtil.range() 生成本周的七天日期对象
     * step3 用本周每天日期查询当天考勤情况：
     * s3-1 先判断当天是工作日还是休息日
     * s3-2 未来日子考勤结果为空
     * s3-3 当天考勤结束前 没考勤的结果为空字符串
     * s3-4 工作日没有考勤记录就算矿工
     *
     * @param param
     * @return
     */
//    @Override
//    // 这个方法查询了五六次数据库 效率没有下面的月查询查询一次查询的好 但是不想改了
//    public ArrayList<HashMap> searchWeekCheckin(HashMap param) {
//        //本周特殊的节假日
//        ArrayList<String> holidaysList = holidaysDao.searchHolidaysInRange(param);
//        // 本周特殊工作日
//        ArrayList<String> workDayList = workdayDao.searchWorkDayInRange(param);
//        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
//        DateTime endDate = DateUtil.parse(param.get("endDate").toString());
//        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);
//        ArrayList list = new ArrayList();
//
//
//        // 每一天都要判断 是不是按时完成任务->status 是不是工作日->type 当天日期->date 周->day
//        range.forEach(one -> {
//            String date = one.toString("yyyy-MM-dd");
//            //查看今天是不是假期或者工作日
//            String type = "工作日";
//            if (one.isWeekend()) {
//                type = "节假日";
//            }
//            if (holidaysList != null && holidaysList.contains(date)) {
//                type = "节假日";
//            } else if (workDayList != null && workDayList.contains(date)) {
//                type = "工作日";
//            }
//
//            String status = "";
//            // 这周到今天之前的日子
//            DateTime endTime = DateUtil.parse(DateUtil.tomorrow().toDateStr() + " " + systemConstants.attendanceEndTime);
//            String today = DateUtil.today();
//            if (date.equals(today) && DateUtil.date().isBefore(endTime)) {
//                status = "";
//            }
          //            else if (DateUtil.compare(one, DateUtil.date()) <= 0) {
//                status = "正常";
//                boolean flag = false;
//                HashMap map = new HashMap<>();
//                map.put("userId",param.get("userId"));
//                map.put("date",date);
//                HashMap oneCheck = checkinDao.searchCheckInAndOutByDate(map);
//
//                if (oneCheck == null){
//                    status = "缺勤";
//                }
//                else {
//                    // 此时 肯定有上班打卡状态 1为正常 2为迟到
//                    // 但不一定有下班打卡时间和工作量
//                    Integer checkInStatus = (Integer) oneCheck.get("checkInStatus");
//                    Integer checkOutStatus = (Integer) oneCheck.get("checkOutStatus");
//                    Integer worktime = (Integer) oneCheck.get("workTime");
//                    if (Integer.compare(checkInStatus, 1) != 0){
//                        status = "迟到";
//                    }
//                    // 未下班签到
//                    if (checkOutStatus==null || Integer.compare(checkOutStatus, 1) != 0){
//                        status = "早退";
//                    }else if (Integer.compare(checkOutStatus, 1) == 0){
//                        status = "正常";
//                        if (Integer.compare(worktime, 60* 10) > 0){
//                            status = "加班";
//                        }
//                    }
//                }
//
//
//            }
//            HashMap map = new HashMap();
//            map.put("date", date);
//            map.put("status", status);
//            map.put("type", type);
//            map.put("day", one.dayOfWeekEnum().toChinese("周"));
//            list.add(map);
//        });
//        return list;
//    }


    @Override
    public ArrayList<HashMap> searchMonthCheckin(HashMap param) {
        // 用户本周考勤情况
        // 月查询查询一次数据库 周查询查询多次数据库 月查询如果查询30次 太浪费了
        ArrayList<HashMap> checkInList = checkinDao.searchMonthCheckin(param);
        //本周特殊的节假日
        ArrayList<String> holidaysList = holidaysDao.searchHolidaysInRange(param);
        // 本周特殊工作日
        ArrayList<String> workDayList = workdayDao.searchWorkDayInRange(param);
        DateTime startDate = DateUtil.parseDate(param.get("startDate").toString());
        // 注意这里不能写parseDate 要写parse
        DateTime endDate = DateUtil.parse(param.get("endDate").toString());
        DateRange range = DateUtil.range(startDate, endDate, DateField.DAY_OF_MONTH);
        ArrayList list = new ArrayList();
        range.forEach(one -> {
            String date = one.toString("yyyy-MM-dd");
            //查看今天是不是假期或者工作日
            String type = "工作日";
            if (one.isWeekend()) {
                type = "节假日";
            }
            if (holidaysList != null && holidaysList.contains(date)) {
                type = "节假日";
            } else if (workDayList != null && workDayList.contains(date)) {
                type = "工作日";
            }
            String status = "";
            if (type.equals("工作日") && DateUtil.compare(one, DateUtil.date()) <= 0) {
                status = "缺勤";
                boolean flag = false;

                for (HashMap<String, Object> map : checkInList) {
                    if (date.equals(map.get("ciDate"))) {
                        // 此时 肯定有上班打卡状态 1为正常 2为迟到
                        // 但不一定有下班打卡时间和工作量
                        Integer checkInStatus = (Integer) map.get("checkInStatus");
                        Integer checkOutStatus = (Integer) map.get("checkInStatus");
                        Integer workTime = (Integer) map.get(map.get("workTime"));
                        if (Integer.compare(checkInStatus, 1) != 0){
                            status = "迟到";
                        }
                        // 未下班签到
                        if (checkOutStatus==null || Integer.compare(checkOutStatus, 1) != 0){
                            status = "早退";
                        }else if (Integer.compare(checkOutStatus, 1) == 0){
                            status = "正常";
                            if (Integer.compare(workTime, 60* 10) > 0){
                                status = "加班";
                            }
                        }
                        flag = true;
                        break;
                    }
                    DateTime endTime = DateUtil.parse(DateUtil.tomorrow().toDateStr() + " " + systemConstants.attendanceEndTime);
                    String today = DateUtil.today();
                    if (date.equals(today) && DateUtil.date().isBefore(endTime) && flag == false) {
                        status = "";
                    }
                }
            }
            HashMap map = new HashMap();
            map.put("date", date);
            map.put("status", status);
            map.put("type", type);
            map.put("day", one.dayOfWeekEnum().toChinese("周"));
            list.add(map);
        });
        return list;
    }

    /**
     * 补签
     * @param
     */
    @Override
    public void insertReCheck(TbRecheck entity) {
        // 根据当前补签表 判断是否签过到 以及是否存在补签表中
        if (!this._checkCheckIsExistByDate(entity)){
            // FIXME 如果存在 其实可以删掉重新 创
            if (!this._checkCheckIsExistReCHeckByDate(entity)){
                recheckDao.insert(entity);
            }else {
                throw new EmosException("以补当日签");
            }
        }
        else {
            throw new EmosException("当日已打卡 不可补签");
        }
    }

    @Override
    public void updateReCheckStatus(int userId, Integer reCheckId, Integer status) {
        // 1 拿到补充表
        TbRecheck recheck = recheckDao.selectByPrimaryKey(reCheckId);
        // 先看看打卡了没
        boolean b = this._checkCheckIsExistByDate(recheck);
        if (!b){
            int inOrOut = recheck.getInorout();
            // 可以补签 先判断状态前端出过来的状态码 和数据库状态码不同
            //前后端匹配  1->1 11->(12) 转到本部门(不给其他部门)  0->13 逻辑删除数据表
            if (Integer.compare(status,1) == 0){
                // 同意
                if (inOrOut == 0){
                    _updateReCheckInApprove(recheck,userId);
                }else if (inOrOut == 1){
                    _updateReCheckOutApprove(recheck,userId);
                }
            }
            else if (Integer.compare(status,11)==0){ // 一般不用吧
                // 待处理继续给别人 给权限更高的人
                // 查询权限更高的人 //仅 本部门
                List<HashMap> list = userDao.searchDeptBoss(recheck.getUserId());
                List<Integer> approvers = _filterRelatedInfo(list);
                String approvalIds = TypeChange.ListToString(approvers);
                recheck.setApproverId(approvalIds);
                recheckDao.updateByPrimaryKeySelective(recheck);

            }else if (Integer.compare(status,0)==0) {
                // 不同意
                recheck.setStatus(12);
                recheckDao.updateByPrimaryKeySelective(recheck);
            }
        }else {
            // 说明已经补过签了 删除该表 逻辑删除 状态 13
            recheck.setStatus(13);
            recheckDao.updateByPrimaryKeySelective(recheck);
            throw new EmosException("当日已打卡 不可补签");
        }
    }

    @Override
    public HashMap searchReCheckById(Integer id) {
        return recheckDao.selectReCheckById(id);
    }

    /**
     * 补上班签到
     */
    private void _updateReCheckInApprove(TbRecheck recheck, int userId) {
        TbCheckin entity = new TbCheckin();
        entity.setAddress(recheck.getAddress());
        entity.setStatus((byte) 1);
        entity.setUserId(recheck.getUserId());
        entity.setCreateTime(recheck.getTime());
        entity.setDate(recheck.getDate());

        checkinDao.insert(entity);
        //更改补签表的状态
        _updateReCheckStatus(recheck,"["+userId+"]",1);
    }

    /**
     * 下班签到
     */
    private void _updateReCheckOutApprove(TbRecheck recheck, int userId) {
        TbCheckout entity = new TbCheckout();
        entity.setAddress(recheck.getAddress());
        entity.setUserId(recheck.getUserId());
        entity.setCreateTime(recheck.getTime());
        entity.setDate(recheck.getDate());
        entity.setStatus((byte) 1);
        // 多一步 把当前 上班签到表的时间取出来 顺便检查
        String checkInTime = _checkCheckInCreateTimeByDate(recheck.getUserId(), recheck.getDate());
        if (StringUtils.isBlank(checkInTime)) throw new EmosException("当日上班还未签到");
        // 返回格式 dd xx:xx 仅日期 + 时+ 分
        Integer h1 = Integer.valueOf(checkInTime.substring(11, 13));
        Integer m1 = Integer.valueOf(checkInTime.substring(14,16));
        String checkInDay = checkInTime.substring(8,10);
        // 早上的时间
        int t1 = h1 * 60 + m1;

        // 下班签到的时间 2021-07-27 10:34:55
        Date Time = recheck.getCreateTime();
        String checkOutTime = new DateTime(Time).toString();
        Integer h2 = Integer.valueOf(checkOutTime.substring(11, 13));
        Integer m2 = Integer.valueOf(checkOutTime.substring(14,16));
        String checkOutDay = checkOutTime.substring(8,10);
        int t2 = h2 * 60 + m2;
        if (!checkInDay.equals(checkOutDay)){
            t2 += (24 * 60 - t1);
        }
        int workTime = t2 - t1;
        entity.setWorkTime(workTime);
        if (workTime < 8 * 60 && t1 > 9 * 60){
            entity.setStatus((byte) 2);; // 跟下班签到表同步 迟到 且工作量不够
        }else if (workTime>10 * 60) {
            entity.setStatus((byte) 1); // 工作量超了 迟不迟到无所谓
        }
        checkoutDao.insert(entity);
        //更改补签表的状态
        _updateReCheckStatus(recheck,"["+userId+"]",1);

    }

    //更改补签表的状态
    private void _updateReCheckStatus(TbRecheck recheck,String approveId,int status){
        recheck.setStatus(status);
        recheck.setApproverId("["+approveId+"]");
        recheckDao.updateByPrimaryKeySelective(recheck);
    }

    /**
     * 判断当前日期内是否存在checkIn / checkOut 打卡记录
     * @param
     * @return
     */
    private boolean _checkCheckIsExistByDate(TbRecheck entity){
        HashMap pmap = new HashMap();   // 里面装的是上班日期 时间 和userId
        pmap.put("userId", entity.getUserId());
        pmap.put("date", entity.getDate());
        String checkStatus = null;
        if (entity.getInorout() == 0){  // 上午
            checkStatus = checkinDao.getCheckIn(pmap);
        }else if (entity.getInorout() == 1){
            checkStatus = checkoutDao.getCheckOut(pmap);
        }else {
            throw new EmosException("补签订单异常！");
        }
        if (StringUtils.isBlank(checkStatus)){
            return false;
        }else {
            return true;
        }
    }


    /**
     * 判断当前日期内是否存在reCheck表内 打卡记录
     * @param
     * @return
     */
    private boolean _checkCheckIsExistReCHeckByDate(TbRecheck entity){
        HashMap map = new HashMap();
        map.put("date", entity.getDate());
        map.put("userId",entity.getUserId());
        map.put("inOrOut",entity.getInorout());
        String checkStatus = recheckDao.selectByDateAndUserId(map);
        if (StringUtils.isBlank(checkStatus)){
            return false;
        }else {
            return true;
        }
    }


    private List<Integer> _filterRelatedInfo(List<HashMap> list){
        ArrayList<Integer> approvers = new ArrayList<>();
        Iterator<HashMap> iterator = list.iterator();
        while (iterator.hasNext()){
            HashMap next = iterator.next();
            if (next.get("id") != null && !approvers.contains(next.get("id"))) {
                approvers.add(((Long) next.get("id")).intValue());
            }
        }
        return approvers;
    }

    // 检查上班签到表在规定日期内是否存在上班表
    private String _checkCheckInCreateTimeByDate(int userId,String date){
        String start = date + " " + systemConstants.attendanceStartTime;
        String end = date + " " + systemConstants.attendanceEndTime;
        HashMap pmap = new HashMap();   // 里面装的是上班日期 时间和userId
        pmap.put("userId", userId);
        pmap.put("date", date);
        String checkInTime = checkinDao.getCheckIn(pmap);
        return checkInTime; // 返回格式 dd xx:xx 仅日期 + 时+ 分
    }


}

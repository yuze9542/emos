package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.dao.TbMeetingDao;
import com.example.emos.wx.db.dao.TbUserDao;
import com.example.emos.wx.db.pojo.TbMeeting;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MeetingService;
import io.swagger.models.auth.In;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

@Service
public class MeetingServiceImpl implements MeetingService {

    @Autowired
    private TbMeetingDao meetingDao;

    @Autowired
    private TbUserDao userDao;

    @Value("${emos.code}")
    private String code;

    @Value("${workflow.url}")
    private String workflow;

    @Value("${emos.recieveNotify}")
    private String recieveNotify;

    @Override
    public void insertMeeting(TbMeeting meeting) {
        int row = meetingDao.insertMeeting(meeting);
        if (row != 1) {
            throw new EmosException("会议添加失败");
        }
        // 工作流
//        startMeetingWorkflow(meeting.getUuid(), meeting.getCreatorId().intValue(), meeting.getDate(),meeting.getStart());
    }

    @Override
    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap params) {
        ArrayList<HashMap> list = meetingDao.searchMyMeetingListByPage(params); //拿到了会议列表数据
        String date = null;
        ArrayList resultList = new ArrayList();
        HashMap resultMap = null;
        JSONArray array = null;

        Iterator<HashMap> it = list.iterator();
        while (it.hasNext()) {
            HashMap map = it.next();
            DateTime time = DateUtil.parse((String) map.get("date") + " " + (String) map.get("end") + ":00");
            DateTime now = DateUtil.parse(DateUtil.today());
            DateTime delete_time = DateUtil.offsetDay(now, -3); // 负数是往前提
            if (delete_time.isAfterOrEquals(time)) {
                int id = Integer.parseInt(map.get("id").toString());
                _deleteMeetingOver(id);
                it.remove();
            } else if (now.isAfter(time)) {
                int id = Integer.parseInt(map.get("id").toString());
                _updateMeetingOver(id);
                map.put("status", 4);
//                it.remove();
            }
        }
        for (HashMap map : list) {

            String temp = map.get("date").toString();
            // 之前写的date.equals(temp) 导致了空指针异常
            // 拿到的日期如果等于旧日期 把旧日期的小列表加上 map 就行
            // map里面放的是 很多会议的内容
            // 如果拿到的日期不等于旧日期 就说明是新日期
            // 新日期就要new一个新的jsonArray 然后把这个

            if (!temp.equals(date)) {    // 当旧日期不等于新日期时 把
                date = temp;
                resultMap = new HashMap();
                resultMap.put("date", date);
                array = new JSONArray();
                resultMap.put("list", array);
                resultList.add(resultMap);
            }
            //会议小列表 因为每天可能有不同会议 所以需要有个列表存储同一天内的不同会议
            array.put(map);
        }
        return resultList;
    }

    @Override
    public HashMap searchMeetingById(int id) {
        HashMap map = meetingDao.searchMeetingById(id);
        ArrayList<HashMap> list = meetingDao.searchMeetingMembers(id);
        map.put("members", list);
        return map;
    }

    @Override
    public int updateMeetingInfo(HashMap param) {
        int id = (int) param.get("id");
        String date = param.get("date").toString();
        String start = param.get("start").toString();
        String instanceId = param.get("instanceId").toString();
        HashMap oldMeeting = meetingDao.searchMeetingById(id);
        String uuid = oldMeeting.get("uuid").toString();
        Integer creatorId = Integer.parseInt(oldMeeting.get("creatorId").toString());
        int row = meetingDao.updateMeetingInfo(param);
        return row;
    }

    @Override
    public int deleteMeetingInfo(int id) {
        HashMap meeting = meetingDao.searchMeetingById(id); // 查询会议信息
        DateTime time = DateUtil.parse(meeting.get("date") + " " + meeting.get("start"));
        DateTime now = new DateTime();
        if (now.isAfterOrEquals(time.offset(DateField.MINUTE, -20))) {
            throw new EmosException("距离会议不足20分钟 不能删除会议");
        }
        int row = meetingDao.deleteMeetingInfo(id);
        if (row != 1)
            throw new EmosException("删除会议失败");
        return row;
    }

    @Override
    public ArrayList<HashMap> searchMeetingByManagerDept(HashMap param) {
        ArrayList<HashMap> list = meetingDao.searchMeetingByManagerDept(param);
//        String date = null;
//        ArrayList resultList = new ArrayList();
//        HashMap resultMap =null;
//        JSONArray array = null;
//
//        for(HashMap map:list){
//
//            String temp = map.get("date").toString();
//            // 之前写的date.equals(temp) 导致了空指针异常
//            // 拿到的日期如果等于旧日期 把旧日期的小列表加上 map 就行
//            // map里面放的是 很多会议的内容
//            // 如果拿到的日期不等于旧日期 就说明是新日期
//            // 新日期就要new一个新的jsonArray 然后把这个
//
//            if (!temp.equals(date)){    // 当旧日期不等于新日期时 把
//                date=temp;
//                resultMap=new HashMap();
//                resultMap.put("date",date);
//                array = new JSONArray();
//                resultMap.put("list",array);
//                resultList.add(resultMap);
//            }
//            //会议小列表 因为每天可能有不同会议 所以需要有个列表存储同一天内的不同会议
//            array.put(map);
//        }
        return list;
    }

    @Override
    public int approvalMeetingInfo(int flag, int id) {
        if (flag == 0) {
            return meetingDao.updateMeetingSuccessById(id);
        } else if (flag == 1) {
            return meetingDao.updateMeetingFailById(id);
        }

        return -1;
    }

    @Override
    public List<String> searchUserMeetingInMonth(HashMap map) {
        return meetingDao.searchUserMeetingInMonth(map);
    }

    private int _updateMeetingOver(int id) {
        int row = meetingDao.updateMeetingOver(id);
        if (row != 1)
            throw new EmosException("更新会议失败");
        return row;
    }

    private int _deleteMeetingOver(int id) {
        int row = meetingDao.deleteMeetingInfo(id);
        if (row != 1)
            throw new EmosException("删除会议失败");
        return row;
    }


//    private void startMeetingWorkflow(String uuid, int creatorId,String date,String start){
//        HashMap info = userDao.searchUserInfo(creatorId);
//        JSONObject json = new JSONObject();
//        json.set("url",recieveNotify);
//        json.set("uuid",uuid);
//        json.set("openId",info.get("openId"));
//        json.set("code",code);
//        json.set("date",date);
//        json.set("start",start);

//        String[] roles = info.get("roles").toString().split("，");
//        if (!ArrayUtil.contains(roles, "总经理")) {
//            Integer managerId = userDao.searchDeptManagerId(creatorId);
//            json.set("managerId", managerId);   // 部门经理id
//            Integer gmId = userDao.searchGmId();    // 总经理id
//            json.set("gmId", gmId);
//            boolean bool = meetingDao.searchMeetingMembersInSameDept(uuid);
//            json.set("sameDept", bool);
//        }
//        String url = workflow + "/workflow/startMeetingProcess";
//        HttpResponse resp = HttpRequest.post(url).header("Content-Type", "application/json")
//                .body(json.toString()).execute();

//        HashMap param = new HashMap();
//        param.put("uuid", uuid);
//        int row = meetingDao.updateMeetingInstanceId(param);
//        if (resp.getStatus() == 200) {
//            json = JSONUtil.parseObj(resp.body());
//            String instanceId = json.getStr("instanceId");
//            HashMap param = new HashMap();
//            param.put("uuid", uuid);
//            param.put("instanceId", instanceId);
//            int row = meetingDao.updateMeetingInstanceId(param);
//            if (row != 1) {
//                throw new EmosException("保存会议工作流实例ID失败");
//            }
//        }
//    }

}

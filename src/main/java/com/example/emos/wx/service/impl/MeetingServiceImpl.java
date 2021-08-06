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

/**
 *  会议status 1是刚创建 2是审核未通过 3是审核通过但未开始 4是正在进行中 5是结束了 6是已删除
 */
@Service
public class MeetingServiceImpl implements MeetingService {

    @Autowired
    private TbMeetingDao meetingDao;

    @Autowired
    private TbUserDao userDao;


    @Value("${emos.offsetTime}")
    private String offsetTime;


    @Override
    public void insertMeeting(TbMeeting meeting) {
        // 这里把参会人员 放入一个数组里 然后放在member字段
        int row = meetingDao.insertMeeting(meeting);
        if (row != 1) {
            throw new EmosException("会议添加失败");
        }
    }

    @Override
    public ArrayList<HashMap> searchMyMeetingListByPage(HashMap params) {
        // 先根据我的id查出所有会议
        ArrayList<HashMap> list = meetingDao.searchMyMeetingListByPage(params); //拿到了会议列表数据
        String date = null;
        ArrayList resultList = new ArrayList();
        HashMap resultMap = null;
        JSONArray array = null;

        // 这一块代码全是删除会议的和改变会议类型的
        Iterator<HashMap> it = list.iterator();
        while (it.hasNext()) {
            HashMap map = it.next();
            DateTime startTime = DateUtil.parse((String) map.get("date") + " " + (String) map.get("start") + ":00");
            DateTime endTime = DateUtil.parse((String) map.get("date") + " " + (String) map.get("end") + ":00");
            DateTime now = DateUtil.date();
            DateTime delete_time = DateUtil.offsetDay(endTime, Integer.parseInt(offsetTime)); // 负数是往前提
            // 会议时间的3天之后，就逻辑删掉这个会议
            if (now.isAfterOrEquals(delete_time)) {
                int id = Integer.parseInt(map.get("id").toString());
                _updateMeetingToDelete(id); // 改成逻辑删除吧
                    it.remove();    // 不要它了 这也就是为什么用遍历器的原因
            }
            // 如果 现在的时间在会议时间之后
            else if (now.isAfter(endTime)) {
                int id = Integer.parseInt(map.get("id").toString()); // 会议id
                _updateMeetingOver(id);
                map.put("status", 5);   // 代表过期了
//                it.remove();
            }
            // 如果 正在开会时
            else if (now.isAfter(startTime)&&now.isBefore(endTime)){
                int id = Integer.parseInt(map.get("id").toString()); // 会议id
                _updateMeetingToStarting(id);
                map.put("status", 4);
            }
        }

        //
        for (HashMap map : list) {
            //会议日期是肯定有的
            String temp = map.get("date").toString();
            // 之前写的date.equals(temp) 导致了空指针异常 因为date初始为空
            // 拿到的日期如果等于旧日期 把旧日期的小列表加上 map 就行
            // map里面放的是 很多会议的内容
            // 如果拿到的日期不等于旧日期 就说明是新日期
            // 新日期就要new一个新的jsonArray 然后把这个

            // 当旧日期不等于新日期时 把相同日期的会议放入一个临时map里
            if (!temp.equals(date)) {
                date = temp;
                resultMap = new HashMap();
                resultMap.put("date", date);
                array = new JSONArray();
                //  仔细看这段代码 即使先添加到map里 在改value的值 map的数字也会变
                resultMap.put("list", array);
                resultList.add(resultMap);
            }
            // 测试一个小功能 看看查看的这个人和创建人是不是同一个人
            Integer creatorId = Integer.parseInt((String) map.get("u2Id")) ; // 创建人id
            Integer userId = (Integer) params.get("userId");   // 查询人id
            if (Integer.compare(creatorId,userId)==0){
                // 相同 可编辑
                map.put("isSamePeople",true);
            }else {
                map.put("isSamePeople",false);
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
        int row = _deleteMeetingOver(id);
        if (row != 1)
            throw new EmosException("删除会议失败");
        return row;
    }

    @Override
    // 部门经理才能查询啊？ 有权限就行
    public ArrayList<HashMap> searchMeetingByManagerDept(HashMap param) {
        ArrayList<HashMap> list = meetingDao.searchMeetingByManagerDept(param);
        String date = null;
        ArrayList resultList = new ArrayList();
        HashMap resultMap =null;
        JSONArray array = null;

        for(HashMap map:list){

            String temp = map.get("date").toString();
            // 之前写的date.equals(temp) 导致了空指针异常
            if (!temp.equals(date)){    // 当旧日期不等于新日期时 把
                date=temp;
                resultMap=new HashMap();
                resultMap.put("date",date);
                array = new JSONArray();
                resultMap.put("list",array);
                resultList.add(resultMap);
            }
            //会议小列表 因为每天可能有不同会议 所以需要有个列表存储同一天内的不同会议
            array.put(map);
            // resultList > resultMap > array
        }
        return resultList;
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

    // 会议结束了
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
    private int _updateMeetingToDelete(int id) {
        int row = meetingDao.updateMeetingToDelete(id);
        if (row != 1)
            throw new EmosException("逻辑删除会议失败");
        return row;
    }

    private int _updateMeetingToStarting(int id) {
        int row = meetingDao.updateMeetingToStarting(id);
        if (row != 1)
            throw new EmosException("逻辑删除会议失败");
        return row;
    }


}

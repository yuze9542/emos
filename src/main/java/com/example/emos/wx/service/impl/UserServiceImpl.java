package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.example.emos.wx.db.dao.TbDeptDao;
import com.example.emos.wx.db.dao.TbUserDao;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.UserService;
import com.example.emos.wx.task.MessageTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@Scope("prototype")
public class UserServiceImpl implements UserService {

    @Value("${wx.app-id}")
    private String appId;

    @Value("${wx.secret}")
    private String appSecret;

    @Autowired
    private TbUserDao userdao;

    @Autowired
    private MessageTask messageTask;

    @Autowired
    private TbDeptDao deptDao;


    /**
     * 得到openID 通过微信获取的  openID是微信长期支持的字符串
     *
     * @param code
     * @return
     */
    private String getOpenId(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session";
        HashMap map = new HashMap();
        map.put("appid", appId);
        map.put("secret", appSecret);
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String response = HttpUtil.post(url, map);  // 通过这个获得response
        JSONObject json = JSONUtil.parseObj(response);
        String openId = json.getStr("openid");
        if (openId == null || openId.length() == 0) {
            throw new RuntimeException("临时登陆凭证错误");
        }
        return openId;
    }


    @Override
    /**
     * code 是临时的
     */
    public int registerUser(String registerCode, String code, String nickname, String photo) {
        //如果邀请码是000000，代表是超级管理员
        if (registerCode.equals("000000")) {
            //查询超级管理员帐户是否已经绑定
            boolean bool = userdao.haveRootUser();
            if (!bool) {    // 正常是没有绑定
                //把当前用户绑定到ROOT帐户
                String openId = getOpenId(code);    // 通过code得到openID
                HashMap param = new HashMap();
                param.put("openId", openId);
                param.put("nickname", nickname);
                param.put("photo", photo);
                param.put("role", "[0]");   //  其他人要不同权限
                param.put("status", 1);
                param.put("createTime", new Date());
                param.put("root", true);
                param.put("hiredate", DateUtil.date().toString());
                userdao.insert(param);
                int id = userdao.searchIdByOpenId(openId);

                // 绑定了 发件人 没绑定收件人啊
                MessageEntity entity = new MessageEntity();
                entity.setSenderId(14);  // 收件人?
                entity.setUuid(IdUtil.simpleUUID());
                entity.setSenderName("系统消息");
                entity.setMsg("你是老大，请及时更新您的员工个人信息");
                entity.setSendTime(new Date());
                messageTask.sendAsync(id + "", entity);

                return id;  // 这个id应该是后台用的id
            } else {
                //如果root已经绑定了，就抛出异常
                throw new EmosException("无法绑定超级管理员账号");
            }
        }
        // TODO 普通员工注册
        else {
            return 0;
        }
    }

    @Override
    /**
     * 寻找用户的许可方法
     */
    public Set<String> searchUserPermissions(int userId) {
        Set<String> permissions = userdao.searchUserPermissions(userId);
        return permissions;
    }

    /**
     * 返回用户入职时间
     *
     * @param userId
     * @return
     */
    @Override
    public String searchUserHireDate(int userId) {
        return userdao.searchUserHireDate(userId);
    }

    @Override
    public HashMap searchUserSummary(int userId) {
        return userdao.searchUserSummary(userId);
    }

    @Override
    public Integer login(String code) {
        String openId = getOpenId(code);
        Integer loginId = userdao.searchIdByOpenId(openId);
        if (loginId == null)
            throw new EmosException("账户不存在");
        //FIXME 从消息列表中接收消息
//        messageTask.receiveAsync(loginId + "");
        return loginId;
    }

    @Override
    public TbUser getUserById(int userId) {
        return userdao.selectByUserId(userId);
    }

    @Override
    public ArrayList<HashMap> searchUserGroupByDept(String keyword) {
        // 部门成员？ 根据 员工姓名 查询 部门信息
        ArrayList<HashMap> list1 = deptDao.searchDeptMembers(keyword);
        // 根据部门查询 查询员工信息
        ArrayList<HashMap> list2 = userdao.searchUserGroupByDept(keyword);
        for (HashMap one : list1) {

            long deptId = (long) one.get("id");
            ArrayList members = new ArrayList();
            for (HashMap two : list2) {
                long id = (long) two.get("deptId");  // 员工信息中的部门id
                if (id == deptId) {
                    members.add(two);
                }
            }
            one.put("members", members);
        }
        return list1;
    }

    @Override
    public ArrayList<HashMap> searchMembers(List param) {
        return userdao.searchMembers(param);
    }

    @Override
    public ArrayList<HashMap> searchUserGroupByRole(int roleId) {
        ArrayList<HashMap> UserByRole = userdao.searchUserGroupByRole(roleId);

        ArrayList<HashMap> resultList = new ArrayList<>();
        HashMap resultMap = null;
        JSONArray array = null;
        String dept = null;

        for (HashMap map: UserByRole){
            String temp = map.get("userDept").toString();// 根据部门分组

            if (!temp.equals(dept)){
                dept = temp;
                resultMap = new HashMap();
                resultMap.put("userDept", dept);
                array = new JSONArray();
                resultMap.put("members", array);
                resultList.add(resultMap);
            }
            map.put("selected", (map.get("selected").toString().equals("1"))?true:false);
            array.put(map);
        }
        return resultList;
    }

    @Override
    @Transactional
    public void saveRoleByUserId(Integer roleId,
                                 Map<String, Boolean> parse,
                                 List<Integer> changedUser) {
        // 第一步 拿到要改的 user
        for (int i = 0; i < changedUser.size(); i++) {
            Integer userId = changedUser.get(i);
            Boolean flag = parse.get(userId+ "");
            _updateRoleByUserId(userId,roleId,flag);
        }

    }

    @Override
    public ArrayList<HashMap> searchUserAll(Integer page, Integer length) {
        HashMap<String, Integer> tempMap = new HashMap<>();
        tempMap.put("page",page);
        tempMap.put("length",length);
        List<HashMap> lists = userdao.searchUserAll(tempMap);

        ArrayList<HashMap> resultList = new ArrayList<>();
        HashMap resultMap = null;
        JSONArray array = null;
        String dept = null;

        for (HashMap map: lists){
            String temp = map.get("userDept").toString();// 根据部门分组

            if (!temp.equals(dept)){
                dept = temp;
                resultMap = new HashMap();
                resultMap.put("userDept", dept);
                array = new JSONArray();
                resultMap.put("members", array);
                resultList.add(resultMap);
            }
            array.put(map);
        }
        return resultList;
    }

    /**
     * 根据user Id 寻找1 本部门相关 领导 级别比他高 2人事部领导 3公司高层
     * @param userId
     */
    @Override
    public List<Integer> searchRelatedIdsByUserId(int userId, List<String> departmentNames) {
        // 1寻找userId的相关信息
        List<HashMap> list = userdao.searchDeptBoss(userId);
        // 部门成员信息
        departmentNames.forEach(one->{
            List<HashMap> adDMap = deptDao.searchMembersByDeptName(one);
            adDMap.forEach(o->{
                if (!list.contains(one)) list.add(o);
            });
        });
        // 这里面是一些信息 包括上司的 id 姓名 部门名称 和等级 这些人可以是我的确认签到人员
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

    @Override
    public List<HashMap> searchReCheckListById(int userId) {
        List<HashMap> list = userdao.searchReCheckListById(userId);

        ArrayList<HashMap> resultLists = new ArrayList<>();
        HashMap resultMap = null;
        JSONArray array = null;
        String date = null;

        for (HashMap map:list){
            String temp = (String) map.get("date");
            if (!temp.equals(date)){
                date = temp;
                resultMap= new HashMap();
                array = new JSONArray();
                resultLists.add(resultMap);
                resultMap.put("date",date);
                resultMap.put("reCheckList",array);
            }
            array.add(map);
        }
        return resultLists;
    }

    private  void _updateRoleByUserId(Integer userId,Integer roleId, Boolean insertFlag) {
        // TODO 明天测试多线程

        synchronized(this){
            HashMap map = new HashMap();
            map.put("userId",userId);
            map.put("roleId2","$."+ roleId);
            map.put("roleId",roleId);
            boolean existRoleId = userdao.isExistRoleId(map);
            // 先做判断 如果 存在 existRoleId
            // 并且insertFlag 为真 不在修改
            if (insertFlag && !existRoleId){
                // 插入 roleId
                userdao.updateInsertRoleId(map);
            }else if(!insertFlag && existRoleId){
                // 删除 roleId
                // 笨办法 先拿到 roleId 再删除 再插入
                HashMap tempMap =  userdao.selectRoleByUserId(userId);
                String name = (String) tempMap.get("name");
                String role = (String) tempMap.get("role");
                if (JSONUtil.isJsonArray(role)){
                    JSONArray array = JSONUtil.parseArray(role);
                    if (array.size() == 1) throw new EmosException(name+"只有这一个权限了,不能删除");
                    Iterator<Object> o = array.iterator();
                    while (o.hasNext()) {
                        Integer jo = (Integer) o.next();
                        if(Integer.compare(jo,roleId)==0) {
                            o.remove(); //这种方式OK的
                        }
                    }
                    String updateRoleId = array.toString();
                    userdao.updateRoleById(userId, updateRoleId);
                }
            }
        }

    }


}

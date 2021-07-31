package com.example.emos.wx.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpUtil;
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
    private TbUserDao tbUserDao;

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
            boolean bool = tbUserDao.haveRootUser();
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
                tbUserDao.insert(param);
                int id = tbUserDao.searchIdByOpenId(openId);

                MessageEntity entity = new MessageEntity();
                entity.setSenderId(0);
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
        Set<String> permissions = tbUserDao.searchUserPermissions(userId);
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
        return tbUserDao.searchUserHireDate(userId);
    }

    @Override
    public HashMap searchUserSummary(int userId) {
        return tbUserDao.searchUserSummary(userId);
    }

    @Override
    public Integer login(String code) {
        String openId = getOpenId(code);
        Integer loginId = tbUserDao.searchIdByOpenId(openId);
        if (loginId == null)
            throw new EmosException("账户不存在");
        //从消息列表中接收消息
        messageTask.receiveAsync(loginId + "");
        return loginId;
    }

    @Override
    public TbUser getUserById(int userId) {
        return tbUserDao.selectByUserId(userId);
    }

    @Override
    public ArrayList<HashMap> searchUserGroupByDept(String keyword) {
        // 部门成员？ 根据 员工姓名 查询 部门信息
        ArrayList<HashMap> list1 = deptDao.searchDeptMembers(keyword);
        // 根据部门查询 查询员工信息
        ArrayList<HashMap> list2 = tbUserDao.searchUserGroupByDept(keyword);
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
        return tbUserDao.searchMembers(param);
    }

}

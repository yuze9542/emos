package com.example.emos.wx.service.impl;

import cn.hutool.json.JSONObject;
import com.example.emos.wx.db.dao.TbRoleDao;
import com.example.emos.wx.db.pojo.TbRole;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private TbRoleDao roleDao;

    @Override
    public ArrayList<HashMap> searchRoleOwnPermission(int id) {
        ArrayList<HashMap> list = roleDao.searchRoleOwnPermission(id);
        list = handleData(list);
        return list;
    }

    @Override
    public void insertRole(TbRole role) {
        int row = roleDao.insertRole(role);
        if (row != 1)
            throw new EmosException("添加角色失败");
    }

    @Override
    public void updateRolePermissions(TbRole role) {
        int row = roleDao.updateRolePermissions(role);
        if (row != 1)
            throw new EmosException("修改角色失败");
    }

    private ArrayList<HashMap> handleData(ArrayList<HashMap> list) {
        ArrayList permsList = new ArrayList();
        ArrayList actionList = new ArrayList();
        HashSet set = new HashSet();
        HashMap data = new HashMap();
        for (HashMap map : list) {
            long permissionId = (long) map.get("id");
            String moduleName = (String) map.get("moduleName");
            String actionName = (String) map.get("actionName");
            String selected = map.get("selected").toString();
            if (set.contains(moduleName)) {
                JSONObject json = new JSONObject();
                json.set("id", permissionId);
                json.set("actionName", actionName);
                json.set("selected", selected.equals("1") ? true : false);
                actionList.add(json);
            } else {
                set.add(moduleName);
                data = new HashMap();
                data.put("moduleName", moduleName);
                actionList = new ArrayList();
                JSONObject json = new JSONObject();
                json.set("id", permissionId);
                json.set("actionName", actionName);
                json.set("selected", selected.equals("1") ? true : false);
                actionList.add(json);
                data.put("action", actionList);
                permsList.add(data);
            }
        }
        return permsList;
    }
}

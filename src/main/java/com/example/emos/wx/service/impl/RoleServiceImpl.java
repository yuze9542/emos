package com.example.emos.wx.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.example.emos.wx.db.dao.TbRoleDao;
import com.example.emos.wx.db.pojo.TbRole;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.RoleService;
import com.google.gson.JsonArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private TbRoleDao roleDao;

    /**
     * 根据 角色role id 查询拥有的操作
     * @param id
     * @return
     */
    @Override
    public ArrayList<HashMap> searchRoleOwnPermission(int id) {
        // 拿到了 角色表的权限们
        ArrayList<HashMap> list = roleDao.searchRoleOwnPermission(id);
        list = handleData(list);
        return list;
    }

    public void insertRole(TbRole role) {
        int row = roleDao.insert(role);
        if (row != 1)
            throw new EmosException("添加角色失败");
    }

    @Override
    public void updateRolePermissions(TbRole role) {
        int row = roleDao.updateRolePermissions(role);
        if (row != 1)
            throw new EmosException("修改角色失败");
    }

    @Override
    public ArrayList<HashMap> searchAllRole(Integer page, Integer length) {
        HashMap<String, Integer> map = new HashMap<>();
        map.put("page",page);
        map.put("length",length);
        return roleDao.searchAllRole(map);
    }

    @Override
    public ArrayList<HashMap> searchAllPermission(Integer id) {
        ArrayList<HashMap> lists = roleDao.searchAllPermission();
        HashMap resultMap = null;
        ArrayList resultList = new ArrayList();
        String moduleName = null;
        JSONArray array = null;

        for (HashMap map : lists){
            String tempName = map.get("moduleName").toString();
            if (!tempName.equals(moduleName)) {
                moduleName = tempName;
                resultMap = new HashMap();
                resultMap.put("moduleName", moduleName);
                array = new JSONArray();
                resultMap.put("action", array);
                resultList.add(resultMap);
            }
            map.put("selected",false);
            array.add(map);
            // 说明 resultList -> resultMap -> array
        }

        return resultList;
    }

    @Override
    // 逻辑删除
    public void deleteRoleById(Integer id) {
        roleDao.updateDeletePrimaryKey(id);
    }

    private ArrayList<HashMap> handleData(ArrayList<HashMap> list) {
        ArrayList permsList = new ArrayList();
        ArrayList actionList = new ArrayList();
        HashSet set = new HashSet();
        HashMap data = new HashMap();
        // 遍历拥有的权限  permission表
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

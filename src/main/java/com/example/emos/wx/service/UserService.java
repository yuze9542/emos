package com.example.emos.wx.service;

import cn.hutool.json.JSONArray;
import com.example.emos.wx.db.pojo.TbUser;

import java.util.*;

public interface UserService {
    public int registerUser(String registerCode, String code, String nickname, String photo);

    // 根据用户id查询有哪些权限 例如补签功能 如果有这个功能
    Set<String> searchUserPermissions(int userId);

    public String searchUserHireDate(int userId);

    public HashMap searchUserSummary(int userId);

    // 登录功能
    public Integer login(String code);

    //通过userId查找用户信息
    public TbUser getUserById(int userId);

    public ArrayList<HashMap> searchUserGroupByDept(String keyword);

    public ArrayList<HashMap> searchMembers(List param);

    // 根据roleId查询所有有这个权限的人员
    public ArrayList<HashMap> searchUserGroupByRole(int roleId);

    public void saveRoleByUserId(Integer roleId, Map<String, Boolean> parse, List<Integer> changedUser);

    // 查询所有员工
    public ArrayList<HashMap> searchUserAll(Integer page, Integer length);

    // 查找用户领导和 根据名字查询查询到的部门同事 例如人事部
    public List<Integer> searchRelatedIdsByUserId(int userId,List<String> departmentNames);

    // 寻找补签表中 审批人包含自己的
    public List<HashMap> searchReCheckListById(int userId);

}

package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.shiro.crypto.hash.Hash;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

@Mapper
public interface TbUserDao {
    //    查找openID
    Integer searchIdByOpenId(String openId);

    //   是否有超级管理员
    boolean haveRootUser();

    Set<String> searchUserPermissions(int userId);

    int deleteByPrimaryKey(Integer id);

    HashMap searchUserSummary(int userId);

    int insert(HashMap hashMap);

    String searchUserHireDate(int userId);

    TbUser selectByPrimaryKey(Integer id);

    TbUser selectByUserId(int UserId);

    HashMap searchNameAndDept(int userId);

    int updateByPrimaryKeySelective(TbUser record);

    int updateByPrimaryKey(TbUser record);

    ArrayList<HashMap> searchUserGroupByDept(String keyword);

    ArrayList<HashMap> searchUserGroupByDept(List param);

    ArrayList<HashMap> searchMembers(List param);

    HashMap searchUserInfo(int userId);

    int searchDeptManagerId(int id);        // 查询部门经理id

    //
    int searchGmId();               // 查询经理id

}
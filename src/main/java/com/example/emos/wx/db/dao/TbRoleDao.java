package com.example.emos.wx.db.dao;

import com.example.emos.wx.db.pojo.TbRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.HashMap;

@Mapper
public interface TbRoleDao {
    ArrayList<HashMap> searchRoleOwnPermission(int id);

    int updateRolePermissions(TbRole role);

    int insertRole(TbRole role);
}
package com.example.emos.wx.service;

import com.example.emos.wx.db.pojo.TbRole;

import java.util.ArrayList;
import java.util.HashMap;

public interface RoleService {
    public ArrayList<HashMap> searchRoleOwnPermission(int id);

    public void insertRole(TbRole role);

    public void updateRolePermissions(TbRole role);
}

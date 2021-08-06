package com.example.emos.wx.utils;

import com.example.emos.wx.exception.EmosException;
import lombok.SneakyThrows;

import java.util.ArrayList;
import java.util.List;

public class TypeChange {
    public static String ListToString(List list){
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        list.forEach(one ->{
            sb.append(one);
            sb.append(",");
        });
        sb.deleteCharAt(sb.length()-1);
        sb.append("]");
        return sb.toString();
    }


    static int sum = 1;
}

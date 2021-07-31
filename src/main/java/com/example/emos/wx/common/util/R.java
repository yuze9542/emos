package com.example.emos.wx.common.util;

import org.apache.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;
// 封装Web返回对象

public class R extends HashMap<String, Object> {


    public R() {
        put("code", HttpStatus.SC_OK);
        put("msg", "success");
    }

    public R put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public static R ok() {
        return new R();
    }

    public static R ok(String msg) {
        R r = new R();
        r.put("msg", msg);
        return r;
    }

    public static R ok(Map<String, Object> map) {
        R r = new R();
        r.putAll(map);
        return r;
    }

    //有问题的话会传入一个状态码
    public static R error(int code, String msg) {
        R r = new R();
        r.put("code", code);
        r.put("msg", msg);//覆盖new R()构造函数创造的code和msg
        return r;
    }

    public static R error(String msg) {
        R r = new R();
        r.put("code", 500);
        r.put("msg", msg);//覆盖new R()构造函数创造的code和msg
        return r;
    }

}

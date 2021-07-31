package com.example.emos.wx.config.shiro;

import org.springframework.stereotype.Component;

/**
    ThreadLocalToken 是自定义的类，里面包含了 ThreadLocal 类型的变量，可以用来保存线程
    安全的数据，而且避免了使用线程锁。
 * 从OAuth2Filter 写入token 到这里
 * 然后从aop判断令牌更新
 * 作用 第三方媒介 把token传来传去 http://140.143.132.225:8000/project-1/doc-24/
 */
@Component
public class ThreadLocalToken {
    private ThreadLocal local = new ThreadLocal();


    //这几个方法是 ThreadLocal 常用的几个方法 https://www.cnblogs.com/fsmly/p/11020641.html
    public void setToken(String token) {
        local.set(token);// 设置本地变量的值
    }

    public String getToken() {
        return (String) local.get();
    }

    public void clear() {
        local.remove();
    }
}

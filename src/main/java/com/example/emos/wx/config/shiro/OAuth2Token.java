package com.example.emos.wx.config.shiro;


import org.apache.shiro.authc.AuthenticationToken;

/**
 * 客户端提交的Token不能直接交给Shiro框架，需要先封装成 AuthenticationToken 类型的对象，
 * 将token封装成AuthenticationToken 以便传给shiro做认证与授权
 */
public class OAuth2Token implements AuthenticationToken {
    // 就是普通token 封装了一下 并设置了get set 方法
    private String token;

    public OAuth2Token(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}

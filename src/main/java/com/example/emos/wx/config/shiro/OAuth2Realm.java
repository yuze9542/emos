package com.example.emos.wx.config.shiro;

import com.auth0.jwt.JWT;
import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;


/**
 * 在这个实现类中定义认证和授权的方法
 * 拿到数据后交给shiro
 */
@Component
public class OAuth2Realm extends AuthorizingRealm {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof OAuth2Token;
    }

    // 授权 (验证权限时调用)
    // 如果希望只有满足相关权限的用户才能调用这个Web方法，需要给Web方法添加上 @RequiresPermissions 注解即可
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection collection) {

        TbUser tbUser = (TbUser) collection.getPrimaryPrincipal();// 可以得到用户信息
        int userId = tbUser.getId();

        //用户权限列表
        Set<String> permsSet = userService.searchUserPermissions(userId);
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
        // 这个user相关权限被放到这个里面了 把权限列表添加到info对象中
        info.setStringPermissions(permsSet);

        return info;
    }

    // 认证 (验证登录时调用)
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        //从令牌中获取userId，然后检测该账户是否被冻结。
        // 这个方法在OAuth2Token这个类也写过
        String accessToken = (String) token.getPrincipal();  // 得到token
        int id = jwtUtil.getUserId(accessToken);    // 拿到用户id
        TbUser user = userService.getUserById(id);  // 拿到整个用户的所有信息
        if (user == null) {
            throw new LockedAccountException("账号已被锁定,请联系管理员");
        }
        // 往info对象中添加用户信息、Token字符串
        // 要放 三个参数： 用户信息 token Realm类名字
        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, accessToken, getName());

        return info;
    }
}

package com.example.emos.wx.config.shiro;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
/**
 * 有生成token的方法
 */
public class JwtUtil {

    @Value("${emos.jwt.secret}")
    // 默认123456
    private String secret;

    @Value("${emos.jwt.expire}")
    private int expire; // 令牌过期时间（天）

    /**
     * 通过userId 得到token jwt json web token 携带着信息的token
     *
     * @param userId
     * @return
     */
    public String createToken(int userId) {      // 利用 jwt 创建
        // 计算过期日期
        Date date = DateUtil.offset(new Date(), DateField.DAY_OF_YEAR, expire).toJdkDate();
        // 加密 把默认的123456
        Algorithm algorithm = Algorithm.HMAC256(secret);
        // 创建了一个内部类对象
        JWTCreator.Builder builder = JWT.create();
        // 生成秘钥
        String token = builder
                .withClaim("userId", userId)
                .withClaim("testId","all is 1")
                .withExpiresAt(date).sign(algorithm);
        return token;
    }

    /**
     * 通过token 解码 得到userId
     *
     * @param token
     * @return
     */
    public int getUserId(String token) {     // 得到
        DecodedJWT jwt = JWT.decode(token);//解码
        int userId = jwt.getClaim("userId").asInt();// 那理论上是不是也能拿到testId
        return userId;

    }

    public void verifierToken(String token) {    //  验证
        Algorithm algorithm = Algorithm.HMAC256(secret);
        JWTVerifier verifier = JWT.require(algorithm).build();
        verifier.verify(token);
    }

}

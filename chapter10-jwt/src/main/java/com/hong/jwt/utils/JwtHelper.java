package com.hong.jwt.utils;

import com.hong.jwt.domain.LoginInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mobile.device.Device;
import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * jwt及解析jwt的帮助类.
 * Created by hong on 2017/6/11.
 */
@Component
public class JwtHelper {


    static final String CLAIM_KEY_USERNAME = "sub";
    static final String CLAIM_KEY_AUDIENCE = "audience";
    static final String CLAIM_KEY_CREATED = "created";

    private static final String AUDIENCE_UNKNOWN = "unknown";
    private static final String AUDIENCE_WEB = "web";
    private static final String AUDIENCE_MOBILE = "mobile";
    private static final String AUDIENCE_TABLET = "tablet";

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpiration() {
        return expiration;
    }

    public void setExpiration(Long expiration) {
        this.expiration = expiration;
    }


    public  Claims getClaimsFromToken(String token){
        Claims claims;
        try {
            claims = Jwts.parser()
                    .setSigningKey(secret)
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            claims = null;
        }
        return claims;
    }


    /**
     * 生成token.
     * @param username 用户名
     * @param device  org.springframework.mobile.device 设备判断对象
     * @return
     */
    public  String createJWT(String username, Device device) {
        Map<String, Object> claims = new HashMap<>();
        claims.put(CLAIM_KEY_USERNAME, username);
        claims.put(CLAIM_KEY_AUDIENCE, generateAudience(device));
        claims.put(CLAIM_KEY_CREATED, new Date());
        return generateToken(claims);
    }


    private  String generateToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setExpiration(generateExpirationDate())
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }

    /**
     * 生成token 时间 = 当前时间+ expiration（properties中配置的失效时间）
     * @return
     */
    private Date generateExpirationDate() {
        return new Date(System.currentTimeMillis() + expiration * 1000);
    }

    /**
     * 通过spring-mobile-device 的device 检测访问主体.
     * @param device
     * @return
     */
    private  String generateAudience(Device device) {
        String audience = AUDIENCE_UNKNOWN;
        if (device.isNormal()) {
            audience = AUDIENCE_WEB;//Pc端
        } else if (device.isTablet()) {
            audience = AUDIENCE_TABLET;//Pc端
        } else if (device.isMobile()) {
            audience = AUDIENCE_MOBILE;//平板
        }
        return audience;
    }

    /**
     * 通过token 获取用户名.
     * @param authToken
     * @return
     */
    public String getUsernameFromToken(String authToken) {
        String username;
        try {
            final Claims claims = getClaimsFromToken(authToken);
            username = claims.getSubject();
        } catch (Exception e) {
            username = null;
        }
        return username;
    }


    /**
     * 判断token 失效时间是否到了
     * @param token
     * @return
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }


    /**
     * 获取设置的token 失效时间.
     * @param token
     * @return
     */
    public Date getExpirationDateFromToken(String token) {
        Date expiration;
        try {
            final Claims claims = getClaimsFromToken(token);
            expiration = claims.getExpiration();
        } catch (Exception e) {
            expiration = null;
        }
        return expiration;
    }


    /**
     * Token失效校验.
     * @param token token字符串
     * @param loginInfo 用户信息
     * @return
     */
    public Boolean validateToken(String token, LoginInfo loginInfo) {
        //1.校验签名是否正确
        //2.token是否过期
        //......
        final String username = getUsernameFromToken(token);
        return (
                username.equals(loginInfo.getUsername())
                        && !isTokenExpired(token));
    }
}

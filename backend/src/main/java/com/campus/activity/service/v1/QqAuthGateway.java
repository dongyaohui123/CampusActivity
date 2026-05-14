package com.campus.activity.service.v1;

/**
 * QQ authentication gateway for mini-program code exchange.
 */
public interface QqAuthGateway {
    /**
     * Exchange one-time js code for openid.
     *
     * @param code mini-program login code from qq.login
     * @return openid
     */
    String exchangeCodeForOpenid(String code);
}

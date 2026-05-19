package com.campus.activity.service.v1;

/**
 * WeChat authentication gateway for mini-program code exchange.
 */
public interface WechatAuthGateway {
    /**
     * Exchange one-time js code for openid.
     *
     * @param code mini-program login code from wx.login
     * @return openid
     */
    String exchangeCodeForOpenid(String code);
}

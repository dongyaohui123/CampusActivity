package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.auth.ForgotPasswordRequest;
import com.campus.activity.dto.v1.auth.LoginRequest;
import com.campus.activity.dto.v1.auth.QqLoginRequest;
import com.campus.activity.dto.v1.auth.RegisterRequest;
import com.campus.activity.dto.v1.auth.WechatLoginRequest;
import com.campus.activity.view.v1.LoginUserView;

/**
 * V1AuthService服务接口。
 */
public interface V1AuthService {
    /**
     * 登录并返回当前用户信息。
     *
     * @param request 登录请求
     * @return 登录用户信息
     */
    LoginUserView login(LoginRequest request);

    /**
     * 注册并返回当前用户信息。
     *
     * @param request 注册请求
     * @return 注册后的用户信息
     */
    LoginUserView register(RegisterRequest request);

    /**
     * WeChat mini-program login.
     *
     * @param request wechat login request
     * @return login user view
     */
    LoginUserView wechatLogin(WechatLoginRequest request);

    /**
     * QQ mini-program login.
     *
     * @param request qq login request
     * @return login user view
     */
    LoginUserView qqLogin(QqLoginRequest request);

    /**
     * 通过用户名和手机号验证身份后重置密码。
     *
     * @param request 重置密码请求
     */
    void resetPassword(ForgotPasswordRequest request);

    /**
     * 发送短信验证码（模拟模式：验证码输出到日志）。
     *
     * @param phone 手机号
     */
    void sendSmsCode(String phone);
}

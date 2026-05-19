package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.v1.WechatAuthGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Gateway implementation for WeChat mini-program code2session.
 */
@Component
public class WechatAuthGatewayImpl implements WechatAuthGateway {
    private static final String CODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";

    private final String appid;
    private final String secret;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public WechatAuthGatewayImpl(
            @Value("${wechat.miniapp.appid:}") String appid,
            @Value("${wechat.miniapp.secret:}") String secret,
            ObjectMapper objectMapper
    ) {
        this.appid = appid;
        this.secret = secret;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public String exchangeCodeForOpenid(String code) {
        if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat miniapp config is missing");
        }
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat login code is invalid");
        }

        String url = CODE2SESSION_URL
                + "?appid=" + encode(appid)
                + "&secret=" + encode(secret)
                + "&js_code=" + encode(code)
                + "&grant_type=authorization_code";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(6))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat login request interrupted");
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat login request failed");
        }

        if (response.statusCode() != 200) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "wechat login failed with status: " + response.statusCode()
            );
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.body());
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat login response parse failed");
        }

        int errcode = root.path("errcode").asInt(0);
        if (errcode != 0) {
            String errmsg = root.path("errmsg").asText("wechat login failed");
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "wechat login failed: " + errmsg + " (errcode=" + errcode + ")"
            );
        }

        String openid = root.path("openid").asText("");
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "wechat login failed: openid missing");
        }
        return openid;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

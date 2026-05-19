package com.campus.activity.service;

/**
 * 电子票生成与二维码渲染服务。
 */
public interface RegistrationTicketService {

    /**
     * 生成随机票码。
     */
    String generateTicketCode();

    /**
     * 组装二维码原文。
     */
    String buildQrContent(Long activityId, String ticketCode);

    /**
     * 渲染二维码 PNG。
     */
    byte[] renderQrCode(String content);
}

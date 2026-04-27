package com.campus.activity.service.impl;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.RegistrationTicketService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;

/**
 * 电子票与二维码实现。
 */
@Service
public class RegistrationTicketServiceImpl implements RegistrationTicketService {
    private static final String QR_PREFIX = "CA-TICKET";
    private static final int QR_SIZE = 320;
    private static final int COLOR_DARK = 0xFF111827;
    private static final int COLOR_LIGHT = 0xFFFFFFFF;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateTicketCode() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02X", value));
        }
        return builder.toString();
    }

    @Override
    public String buildQrContent(Long activityId, String ticketCode) {
        return QR_PREFIX + "|" + activityId + "|" + ticketCode;
    }

    @Override
    public byte[] renderQrCode(String content) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE);
            BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? COLOR_DARK : COLOR_LIGHT);
                }
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to render ticket qrcode");
        }
    }
}

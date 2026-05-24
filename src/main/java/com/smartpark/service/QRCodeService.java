package com.smartpark.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.smartpark.model.Booking;
import com.smartpark.model.MonthlyPass;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Service tạo QR Code cho Booking và MonthlyPass
 */
@Service
public class QRCodeService {

    private static final int QR_SIZE = 300;

    /**
     * Tạo QR code cho vé đặt trước (Booking)
     * Format: SMARTPARK_BOOKING|id|code|plate|date|slot
     */
    public String generateBookingQR(Booking booking) throws WriterException, IOException {
        String data = String.format(
            "SMARTPARK_BOOKING|%d|%s|%s|%s|%s",
            booking.getId(),
            booking.getPaymentCode(),
            booking.getLicensePlate(),
            booking.getBookingDate() != null ? booking.getBookingDate().toString() : "",
            booking.getSlotId() != null ? booking.getSlotId() : ""
        );
        
        return generateQRCodeImage(data);
    }

    /**
     * Tạo QR code cho thẻ tháng (MonthlyPass)
     * Format: SMARTPARK_PASS|id|code|plate|endDate
     */
    public String generatePassQR(MonthlyPass pass) throws WriterException, IOException {
        String data = String.format(
            "SMARTPARK_PASS|%d|%s|%s|%s",
            pass.getId(),
            pass.getPaymentCode(),
            pass.getLicensePlate(),
            pass.getEndDate().toString()
        );
        
        return generateQRCodeImage(data);
    }

    /**
     * Tạo QR code image từ text data
     * @return Base64 encoded PNG image (data:image/png;base64,...)
     */
    private String generateQRCodeImage(String data) throws WriterException, IOException {
        // Cấu hình QR code
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        // Tạo QR code matrix
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
            data,
            BarcodeFormat.QR_CODE,
            QR_SIZE,
            QR_SIZE,
            hints
        );

        // Convert sang PNG image
        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();

        // Encode base64 để embed vào HTML
        String base64Image = Base64.getEncoder().encodeToString(pngData);
        return "data:image/png;base64," + base64Image;
    }

    /**
     * Parse QR code data để verify
     * @return Map với keys: type, id, code, plate, etc.
     */
    public Map<String, String> parseQRData(String qrData) {
        Map<String, String> result = new HashMap<>();
        
        if (qrData == null || qrData.isEmpty()) {
            return result;
        }

        String[] parts = qrData.split("\\|");
        
        if (parts.length < 4) {
            return result;
        }

        String type = parts[0];
        result.put("type", type);

        if ("SMARTPARK_BOOKING".equals(type) && parts.length >= 6) {
            result.put("id", parts[1]);
            result.put("code", parts[2]);
            result.put("plate", parts[3]);
            result.put("date", parts[4]);
            result.put("slot", parts[5]);
        } else if ("SMARTPARK_PASS".equals(type) && parts.length >= 5) {
            result.put("id", parts[1]);
            result.put("code", parts[2]);
            result.put("plate", parts[3]);
            result.put("endDate", parts[4]);
        }

        return result;
    }

    /**
     * Kiểm tra QR data có hợp lệ không
     */
    public boolean isValidQRData(String qrData) {
        if (qrData == null || qrData.isEmpty()) {
            return false;
        }

        return qrData.startsWith("SMARTPARK_BOOKING|") || 
               qrData.startsWith("SMARTPARK_PASS|");
    }
}

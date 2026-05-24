package com.smartpark.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response khi verify QR code
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QRVerifyResponse {
    private String type;           // "booking" hoặc "monthly_pass"
    private Long id;
    private String code;
    private String licensePlate;
    private String status;
    private String message;
    private Object details;        // Thông tin chi tiết (Booking hoặc MonthlyPass)
    
    public static QRVerifyResponse success(String type, Long id, String code, 
                                          String plate, String status, Object details) {
        return new QRVerifyResponse(type, id, code, plate, status, "QR hợp lệ", details);
    }
    
    public static QRVerifyResponse error(String message) {
        QRVerifyResponse response = new QRVerifyResponse();
        response.setMessage(message);
        response.setStatus("INVALID");
        return response;
    }
}

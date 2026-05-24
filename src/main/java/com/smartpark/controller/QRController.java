package com.smartpark.controller;

import com.smartpark.dto.response.ApiResponse;
import com.smartpark.dto.response.QRVerifyResponse;
import com.smartpark.model.Booking;
import com.smartpark.model.MonthlyPass;
import com.smartpark.service.BookingService;
import com.smartpark.service.MonthlyPassService;
import com.smartpark.service.QRCodeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Controller xử lý QR Code
 */
@RestController
@RequestMapping("/api/qr")
@CrossOrigin(origins = "*")
public class QRController {

    private final QRCodeService qrCodeService;
    private final BookingService bookingService;
    private final MonthlyPassService monthlyPassService;
    private final com.smartpark.service.ParkingService parkingService;

    public QRController(QRCodeService qrCodeService,
                       BookingService bookingService,
                       MonthlyPassService monthlyPassService,
                       com.smartpark.service.ParkingService parkingService) {
        this.qrCodeService = qrCodeService;
        this.bookingService = bookingService;
        this.monthlyPassService = monthlyPassService;
        this.parkingService = parkingService;
    }

    /**
     * Verify QR code (cho nhân viên quét)
     * POST /api/qr/verify
     * Body: { "qrData": "SMARTPARK_BOOKING|123|SP123456|..." }
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<QRVerifyResponse>> verifyQR(
            @RequestBody Map<String, Object> payload) {
        
        String qrData = (String) payload.get("qrData");
        Boolean isManual = (Boolean) payload.getOrDefault("isManual", false);
        
        System.out.println("Received QR Data: " + qrData + " (Manual: " + isManual + ")");
        
        if (qrData == null || qrData.isEmpty()) {
            System.err.println("QR Data is null or empty!");
            return ResponseEntity.ok(ApiResponse.error("QR data không hợp lệ"));
        }

        // Nếu là nhập thủ công hoặc QR data không đúng format chuẩn, thử tìm theo Payment Code
        if (isManual || !qrCodeService.isValidQRData(qrData)) {
            return verifyByPaymentCode(qrData);
        }

        // Parse QR data chuẩn
        Map<String, String> parsed = qrCodeService.parseQRData(qrData);
        String type = parsed.get("type");

        if ("SMARTPARK_BOOKING".equals(type)) {
            return verifyBookingQR(parsed);
        } else if ("SMARTPARK_PASS".equals(type)) {
            return verifyPassQR(parsed);
        }

        return ResponseEntity.ok(ApiResponse.error("Loại QR không xác định"));
    }

    /**
     * Verify QR vé đặt trước
     */
    private ResponseEntity<ApiResponse<QRVerifyResponse>> verifyBookingQR(
            Map<String, String> parsed) {
        
        try {
            Long id = Long.parseLong(parsed.get("id"));
            String code = parsed.get("code");
            
            Booking booking = bookingService.findById(id).orElse(null);
            
            if (booking == null) {
                return ResponseEntity.ok(ApiResponse.error("Vé không tồn tại"));
            }
            
            if (!booking.getPaymentCode().equals(code)) {
                return ResponseEntity.ok(ApiResponse.error("Mã vé không khớp"));
            }
            
            if (!"PAID".equals(booking.getStatus()) && !"CONFIRMED".equals(booking.getStatus())) {
                return ResponseEntity.ok(ApiResponse.error(
                    "Vé chưa thanh toán (Status: " + booking.getStatus() + ")"
                ));
            }
            
            // Kiểm tra ngày đặt
            if (booking.getBookingDate() != null) {
                LocalDate today = LocalDate.now();
                if (booking.getBookingDate().isBefore(today)) {
                    return ResponseEntity.ok(ApiResponse.error("Vé đã hết hạn"));
                }
            }
            
            QRVerifyResponse response = QRVerifyResponse.success(
                "booking",
                booking.getId(),
                booking.getPaymentCode(),
                booking.getLicensePlate(),
                booking.getStatus(),
                booking
            );
            
            return ResponseEntity.ok(ApiResponse.success("Vé hợp lệ", response));
            
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Lỗi xử lý QR: " + e.getMessage()));
        }
    }

    /**
     * Verify QR thẻ tháng
     */
    private ResponseEntity<ApiResponse<QRVerifyResponse>> verifyPassQR(
            Map<String, String> parsed) {
        
        try {
            Long id = Long.parseLong(parsed.get("id"));
            String code = parsed.get("code");
            
            MonthlyPass pass = monthlyPassService.findById(id).orElse(null);
            
            if (pass == null) {
                return ResponseEntity.ok(ApiResponse.error("Thẻ tháng không tồn tại"));
            }
            
            if (!pass.getPaymentCode().equals(code)) {
                return ResponseEntity.ok(ApiResponse.error("Mã thẻ không khớp"));
            }
            
            if (!"ACTIVE".equals(pass.getStatus())) {
                return ResponseEntity.ok(ApiResponse.error(
                    "Thẻ không còn hiệu lực (Status: " + pass.getStatus() + ")"
                ));
            }
            
            // Kiểm tra ngày hết hạn
            LocalDate today = LocalDate.now();
            if (pass.getEndDate().isBefore(today)) {
                return ResponseEntity.ok(ApiResponse.error("Thẻ đã hết hạn"));
            }
            
            QRVerifyResponse response = QRVerifyResponse.success(
                "monthly_pass",
                pass.getId(),
                pass.getPaymentCode(),
                pass.getLicensePlate(),
                pass.getStatus(),
                pass
            );
            
            return ResponseEntity.ok(ApiResponse.success("Thẻ tháng hợp lệ", response));
            
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Lỗi xử lý QR: " + e.getMessage()));
        }
    }

    /**
     * Tìm kiếm và verify theo Payment Code (SP... hoặc MP...)
     */
    private ResponseEntity<ApiResponse<QRVerifyResponse>> verifyByPaymentCode(String code) {
        System.out.println("Verifying by Payment Code: " + code);
        
        if (code.startsWith("SP")) {
            // Tìm Booking
            Booking booking = bookingService.findByPaymentCode(code).orElse(null);
            if (booking == null) {
                return ResponseEntity.ok(ApiResponse.error("Không tìm thấy vé với mã: " + code));
            }
            
            QRVerifyResponse response = QRVerifyResponse.success(
                "booking",
                booking.getId(),
                booking.getPaymentCode(),
                booking.getLicensePlate(),
                booking.getStatus(),
                booking
            );
            return ResponseEntity.ok(ApiResponse.success("Tìm thấy vé đặt trước", response));
            
        } else if (code.startsWith("MP")) {
            // Tìm MonthlyPass
            MonthlyPass pass = monthlyPassService.findByPaymentCode(code).orElse(null);
            if (pass == null) {
                return ResponseEntity.ok(ApiResponse.error("Không tìm thấy thẻ tháng với mã: " + code));
            }
            
            QRVerifyResponse response = QRVerifyResponse.success(
                "monthly_pass",
                pass.getId(),
                pass.getPaymentCode(),
                pass.getLicensePlate(),
                pass.getStatus(),
                pass
            );
            return ResponseEntity.ok(ApiResponse.success("Tìm thấy thẻ tháng", response));
        }
        
        return ResponseEntity.ok(ApiResponse.error("Mã không đúng định dạng (phải bắt đầu bằng SP hoặc MP)"));
    }

    /**
     * Xác nhận xe vào bãi sau khi quét QR thành công
     * POST /api/qr/confirm-entry
     */
    @PostMapping("/confirm-entry")
    public ResponseEntity<ApiResponse<com.smartpark.model.ParkingSlot>> confirmEntry(
            @RequestBody Map<String, String> payload) {
        
        String type = payload.get("type"); // "booking" hoặc "monthly_pass"
        String plate = payload.get("licensePlate");
        String slotId = payload.get("slotId");

        if (slotId == null || slotId.isEmpty() || plate == null || plate.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("Thiếu thông tin ô đỗ hoặc biển số"));
        }

        try {
            com.smartpark.model.ParkingSlot slot = parkingService.checkin(slotId, plate);
            
            // Nếu là booking, cập nhật trạng thái booking thành COMPLETED hoặc tương tự nếu cần
            // Ở đây đơn giản là ghi nhận xe vào ô đỗ
            
            return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận xe " + plate + " vào ô " + slotId, slot));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Lỗi ghi nhận xe vào: " + e.getMessage()));
        }
    }
}

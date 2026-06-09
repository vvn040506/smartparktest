package com.smartpark.controller;

import com.smartpark.model.Booking;
import com.smartpark.model.StaffAccount;
import com.smartpark.service.BookingService;
import com.smartpark.service.PricingService;
import com.smartpark.service.QRCodeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller xử lý các chức năng dành cho nhân viên (Staff)
 * - Walk-in booking (đỗ xe trực tiếp không cần đặt trước)
 * - Hoàn thành walk-in (tính tiền khi xe ra)
 */
@Controller
@RequestMapping("/staff")
public class StaffController {

    @Autowired private BookingService bookingService;
    @Autowired private PricingService pricingService;
    @Autowired private QRCodeService qrCodeService;

    /**
     * Kiểm tra staff đã đăng nhập chưa
     */
    private StaffAccount getAuthenticatedStaff(HttpSession session) {
        StaffAccount staff = (StaffAccount) session.getAttribute("user");
        if (staff == null) {
            throw new RuntimeException("Vui lòng đăng nhập với tài khoản nhân viên");
        }
        return staff;
    }

    // ═══════════════════════════════════════════════════════════════
    // WALK-IN BOOKING - Tạo vé đỗ xe trực tiếp
    // ═══════════════════════════════════════════════════════════════

    /**
     * Hiển thị trang walk-in booking
     */
    @GetMapping("/walk-in")
    public String walkInPage(HttpSession session, Model model) {
        getAuthenticatedStaff(session); // Kiểm tra đăng nhập
        
        // Ước tính giá cho hiển thị
        long motoEstimate = pricingService.estimateWalkInPrice("xe_may");
        long carEstimate = pricingService.estimateWalkInPrice("o_to");
        
        model.addAttribute("motoEstimate", motoEstimate);
        model.addAttribute("carEstimate", carEstimate);
        
        return "staff-walk-in";
    }

    /**
     * API: Tạo walk-in booking
     * POST /staff/api/walk-in
     */
    @PostMapping("/api/walk-in")
    @ResponseBody
    public ResponseEntity<?> createWalkIn(@RequestBody Map<String, String> payload,
                                          HttpSession session) {
        try {
            getAuthenticatedStaff(session);
            
            String licensePlate = payload.get("licensePlate");
            String vehicleType = payload.get("vehicleType");
            String customerName = payload.get("customerName");
            
            // Validate
            if (licensePlate == null || licensePlate.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Biển số xe không được để trống"
                ));
            }
            
            if (vehicleType == null || (!vehicleType.equals("xe_may") && !vehicleType.equals("o_to"))) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Loại xe không hợp lệ"
                ));
            }
            
            // Tạo walk-in booking
            Booking booking = bookingService.createWalkInBooking(
                licensePlate.trim().toUpperCase(),
                vehicleType,
                customerName != null ? customerName.trim() : ""
            );
            
            // Ước tính giá
            long estimatedPrice = pricingService.estimateWalkInPrice(vehicleType);
            
            // Tạo QR Code cho vé walk-in
            String qrCode = qrCodeService.generateWalkInQR(booking);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo vé walk-in thành công");
            response.put("data", Map.of(
                "bookingId", booking.getId(),
                "ticketCode", booking.getPaymentCode(),
                "estimatedPrice", estimatedPrice,
                "checkInTime", booking.getCheckIn().toString(),
                "licensePlate", booking.getLicensePlate(),
                "vehicleType", booking.getVehicleType(),
                "qrCode", qrCode
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    /**
     * API: Hoàn thành walk-in (xe ra, tính tiền)
     * POST /staff/api/walk-in/{bookingId}/complete
     */
    @PostMapping("/api/walk-in/{bookingId}/complete")
    @ResponseBody
    public ResponseEntity<?> completeWalkIn(@PathVariable Long bookingId,
                                            HttpSession session) {
        try {
            getAuthenticatedStaff(session);
            
            Booking booking = bookingService.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));
            
            if (!"walk_in".equals(booking.getBookingType())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Booking này không phải walk-in"
                ));
            }
            
            if ("COMPLETED".equals(booking.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Booking đã hoàn thành rồi"
                ));
            }
            
            // Tính thời gian đỗ
            LocalDateTime checkIn = booking.getCheckIn();
            LocalDateTime checkOut = LocalDateTime.now();
            long hours = Duration.between(checkIn, checkOut).toHours();
            if (hours < 1) hours = 1; // Tối thiểu 1 giờ
            
            // Tính giá
            long finalPrice = pricingService.calculateWalkInPrice(hours, booking.getVehicleType());
            
            // Hoàn thành booking
            Booking completed = bookingService.completeWalkIn(bookingId, finalPrice);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Hoàn thành walk-in thành công");
            response.put("data", Map.of(
                "bookingId", completed.getId(),
                "checkOutTime", completed.getCheckOut().toString(),
                "duration", hours + " giờ",
                "finalPrice", finalPrice,
                "licensePlate", completed.getLicensePlate()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    /**
     * API: Lấy thông tin booking để hoàn thành
     * GET /staff/api/walk-in/{bookingId}
     */
    @GetMapping("/api/walk-in/{bookingId}")
    @ResponseBody
    public ResponseEntity<?> getWalkInBooking(@PathVariable Long bookingId,
                                              HttpSession session) {
        try {
            getAuthenticatedStaff(session);
            
            Booking booking = bookingService.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));
            
            if (!"walk_in".equals(booking.getBookingType())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Booking này không phải walk-in"
                ));
            }
            
            // Tính thời gian đỗ hiện tại
            LocalDateTime checkIn = booking.getCheckIn();
            LocalDateTime now = LocalDateTime.now();
            long hours = Duration.between(checkIn, now).toHours();
            if (hours < 1) hours = 1;
            
            // Tính giá dự kiến
            long estimatedPrice = pricingService.calculateWalkInPrice(hours, booking.getVehicleType());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "bookingId", booking.getId(),
                "ticketCode", "WI" + booking.getId(),
                "licensePlate", booking.getLicensePlate(),
                "vehicleType", booking.getVehicleType(),
                "customerName", booking.getCustomerName() != null ? booking.getCustomerName() : "",
                "checkInTime", booking.getCheckIn().toString(),
                "status", booking.getStatus(),
                "currentHours", hours,
                "estimatedPrice", estimatedPrice
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
}

package com.smartpark.controller;

import com.smartpark.dto.request.CreateMonthlyPassRequest;
import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.model.Booking;
import com.smartpark.model.MonthlyPass;
import com.smartpark.model.User;
import com.smartpark.service.BookingService;
import com.smartpark.service.MonthlyPassService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.servlet.http.HttpSession;
import com.smartpark.service.ParkingSlotService.SlotStats;

import com.smartpark.service.ParkingService;

/**
 * Controller xử lý đặt vé & hiển thị chi tiết vé.
 */
@Controller
public class BookingController {

    private final BookingService bookingService;
    private final ParkingService parkingService;
    private final MonthlyPassService monthlyPassService;
    private final com.smartpark.service.QRCodeService qrCodeService;

    @Value("${app.bank.account}") private String bankAccount;
    @Value("${app.bank.owner}")   private String bankOwner;
    @Value("${app.bank.name}")    private String bankName;

    public BookingController(BookingService bookingService,
                             ParkingService parkingService,
                             MonthlyPassService monthlyPassService,
                             com.smartpark.service.QRCodeService qrCodeService) {
        this.bookingService     = bookingService;
        this.parkingService     = parkingService;
        this.monthlyPassService = monthlyPassService;
        this.qrCodeService      = qrCodeService;
    }

    // Redirect root
    @GetMapping("/")
    public String index(HttpSession session) {
        if (session.getAttribute("user") != null) {
            com.smartpark.model.StaffAccount staff = (com.smartpark.model.StaffAccount) session.getAttribute("user");
            return "admin".equals(staff.getRole()) ? "redirect:/admin" : "redirect:/staff";
        }
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/customer/dashboard";
        }
        return "index";
    }

    // Trang quét QR cho nhân viên
    @GetMapping("/staff/scan-qr")
    public String staffScanQR() {
        return "staff-scan-qr";
    }

    // Trang đặt vé (booking form)
    @GetMapping("/booking")
    public String booking(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/customer/dashboard";
        }
        return "index";
    }


    // Xử lý form đặt vé
    @PostMapping("/dat-ve")
    public String datVe(
            @RequestParam String customerName,
            @RequestParam String licensePlate,
            @RequestParam String vehicleType,
            @RequestParam(required = false) String slotPreference,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime checkOut,
            Model model) {

        if (checkOut.isBefore(checkIn) || checkOut.isEqual(checkIn)) {
            model.addAttribute("error", "Thời gian ra phải sau thời gian vào");
            return "index";
        }

        Booking b = bookingService.createBooking(customerName, licensePlate, vehicleType, checkIn, checkOut);

        // Tự động giữ ô đỗ nếu khách chọn
        if (slotPreference != null && !slotPreference.isBlank()) {
            parkingService.reserveSlot(slotPreference, licensePlate);
        }

        return "redirect:/ve/" + b.getId();
    }

    // Trang chi tiết vé + QR thanh toán
    @GetMapping("/ve/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        Booking b = bookingService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vé", "id", id));

        model.addAttribute("booking", b);
        model.addAttribute("bankAccount", bankAccount);
        model.addAttribute("bankOwner",   bankOwner);
        model.addAttribute("bankName",    bankName);

        // URL QR VietQR – quét bằng app ngân hàng bất kỳ
        String qrUrl = String.format(
            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
            bankName, bankAccount,
            b.getAmountDue(),
            b.getPaymentCode(),
            bankOwner.replace(" ", "%20")
        );
        model.addAttribute("qrUrl", qrUrl);

        // Nếu đã thanh toán → Tạo QR code vé
        if ("PAID".equals(b.getStatus()) || "CONFIRMED".equals(b.getStatus())) {
            try {
                String bookingQR = qrCodeService.generateBookingQR(b);
                model.addAttribute("bookingQR", bookingQR);
            } catch (Exception e) {
                System.err.println("Lỗi tạo QR vé: " + e.getMessage());
            }
        }

        return "chi-tiet";
    }

    // Kiểm tra trạng thái (dùng cho auto-refresh JS)
    @GetMapping("/api/status/{id}")
    @ResponseBody
    public String checkStatus(@PathVariable Long id) {
        return bookingService.findById(id)
                .map(Booking::getStatus)
                .orElse("NOT_FOUND");
    }
    
    // ══════════════════════════════════════════════════════════════════════════
    // PRE-BOOKING API - Đặt trước vị trí đỗ xe
    // ══════════════════════════════════════════════════════════════════════════
    
    /**
     * API: Tạo pre-booking (yêu cầu đăng nhập)
     * POST /api/bookings/pre-booking
     */
    @PostMapping("/api/bookings/pre-booking")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> createPreBooking(
            @RequestBody com.smartpark.dto.request.PreBookingRequest request,
            HttpSession session) {
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                return org.springframework.http.ResponseEntity.status(401).body(java.util.Map.of(
                    "success", false,
                    "message", "Vui lòng đăng nhập để đặt trước"
                ));
            }
            
            // Validate
            if (request.bookingDate().isBefore(LocalDate.now())) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Ngày đặt không được trong quá khứ"
                ));
            }
            
            if (request.endTime().isBefore(request.startTime()) || 
                request.endTime().equals(request.startTime())) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Giờ kết thúc phải sau giờ bắt đầu"
                ));
            }
            
            // Tạo pre-booking
            Booking booking = bookingService.createPreBooking(user, request);
            
            // Tạo QR thanh toán VietQR
            String qrUrl = String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
                bankName, bankAccount,
                booking.getAmountDue(),
                booking.getPaymentCode(),
                bankOwner.replace(" ", "%20")
            );
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Đặt chỗ thành công");
            response.put("data", java.util.Map.of(
                "bookingId", booking.getId(),
                "paymentCode", booking.getPaymentCode(),
                "amountDue", booking.getAmountDue(),
                "status", booking.getStatus(),
                "qrPaymentUrl", qrUrl,
                "bookingDate", booking.getBookingDate().toString(),
                "startTime", booking.getStartTime().toString(),
                "endTime", booking.getEndTime().toString(),
                "slotId", booking.getSlotId() != null ? booking.getSlotId() : ""
            ));
            
            return org.springframework.http.ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body(java.util.Map.of(
                "success", false,
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API: Lấy danh sách booking của user
     * GET /api/bookings/user/{userId}
     */
    @GetMapping("/api/bookings/user/{userId}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> getUserBookings(
            @PathVariable Long userId,
            HttpSession session) {
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user == null || !user.getId().equals(userId)) {
                return org.springframework.http.ResponseEntity.status(401).body(java.util.Map.of(
                    "success", false,
                    "message", "Không có quyền truy cập"
                ));
            }
            
            List<Booking> bookings = bookingService.getByUser(userId);
            
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "data", bookings
            ));
            
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body(java.util.Map.of(
                "success", false,
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }
    
    /**
     * API: Huỷ booking
     * POST /api/bookings/{bookingId}/cancel
     */
    @PostMapping("/api/bookings/{bookingId}/cancel")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> cancelBooking(
            @PathVariable Long bookingId,
            HttpSession session) {
        try {
            User user = (User) session.getAttribute("currentUser");
            if (user == null) {
                return org.springframework.http.ResponseEntity.status(401).body(java.util.Map.of(
                    "success", false,
                    "message", "Vui lòng đăng nhập"
                ));
            }
            
            Booking booking = bookingService.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));
            
            // Kiểm tra quyền sở hữu
            if (booking.getUser() == null || !booking.getUser().getId().equals(user.getId())) {
                return org.springframework.http.ResponseEntity.status(403).body(java.util.Map.of(
                    "success", false,
                    "message", "Bạn chỉ có thể huỷ booking của mình"
                ));
            }
            
            // Chỉ cho phép huỷ booking PENDING
            if (!"PENDING".equals(booking.getStatus())) {
                return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", "Chỉ có thể huỷ booking đang chờ thanh toán"
                ));
            }
            
            // Huỷ booking
            booking.setStatus("CANCELLED");
            bookingService.findById(bookingId); // Save through repository
            
            // Giải phóng slot nếu có
            if (booking.getSlotId() != null) {
                parkingService.checkout(booking.getSlotId());
            }
            
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of(
                "success", true,
                "message", "Đã huỷ booking thành công"
            ));
            
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.status(500).body(java.util.Map.of(
                "success", false,
                "message", "Lỗi: " + e.getMessage()
            ));
        }
    }

    // ── MONTHLY PASS (trang khách) ────────────────────────────────────────────
    
    // Trang Customer Dashboard
    @GetMapping("/customer/dashboard")
    public String customerDashboard(HttpSession session, Model model) {
        com.smartpark.model.User user = (com.smartpark.model.User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("currentUser", user);
        
        // Lấy danh sách thẻ tháng của user
        List<MonthlyPass> passes = monthlyPassService.findByUser(user);
        model.addAttribute("monthlyPasses", passes);
        
        // Thống kê bãi đỗ xe cho sơ đồ
        addStatsToModel(model);
        
        return "customer-dashboard";
    }

    private void addStatsToModel(Model model) {
        com.smartpark.service.ParkingSlotService.SlotStats s = parkingService.getStats();
        model.addAttribute("total",      s.total());
        model.addAttribute("filled",     s.filled());
        model.addAttribute("empty",      s.empty());
        model.addAttribute("motoTotal",  s.motoTotal());
        model.addAttribute("motoFilled", s.motoFilled());
        model.addAttribute("motoEmpty",  s.motoEmpty());
        model.addAttribute("carTotal",   s.carTotal());
        model.addAttribute("carFilled",  s.carFilled());
        model.addAttribute("carEmpty",   s.carEmpty());
        model.addAttribute("pct",        s.pct());
        model.addAttribute("motoSlots",  parkingService.getSlotsByZone("motorbike"));
        model.addAttribute("carSlots",   parkingService.getSlotsByZone("car"));
        model.addAttribute("motoRows",   List.of("A","B","C","D","E"));
        model.addAttribute("carRows",    List.of("A","B","C"));
    }

    // Trang đăng ký thẻ tháng
    @GetMapping("/the-thang")
    public String monthlyPassPage(HttpSession session) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/customer/dashboard?tab=reg";
        }
        return "redirect:/login";
    }

    // Xử lý form đăng ký thẻ tháng
    @PostMapping("/dang-ky-the-thang")
    public String dangKyTheThang(
            @RequestParam String ownerName,
            @RequestParam(required = false) String email,
            @RequestParam String licensePlate,
            @RequestParam String vehicleType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "1") int months,
            @RequestParam(required = false) String note,
            HttpSession session,
            Model model) {

        com.smartpark.model.User user = (com.smartpark.model.User) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/login";
        }

        if (startDate.isBefore(LocalDate.now())) {
            model.addAttribute("error", "Ngày bắt đầu không được trong quá khứ");
            // Reload stats and passes for the dashboard view in case of error
            model.addAttribute("currentUser", user);
            model.addAttribute("monthlyPasses", monthlyPassService.findByUser(user));
            addStatsToModel(model);
            return "customer-dashboard";
        }

        CreateMonthlyPassRequest req = new CreateMonthlyPassRequest(
                ownerName, email, licensePlate, vehicleType, startDate, months, note);

        MonthlyPass pass = monthlyPassService.create(req, user);
        return "redirect:/the-thang/" + pass.getId();
    }


    // Trang chi tiết thẻ tháng + QR thanh toán
    @GetMapping("/the-thang/{id}")
    public String chiTietTheThang(@PathVariable Long id, Model model) {
        MonthlyPass pass = monthlyPassService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thẻ tháng", "id", id));

        model.addAttribute("pass", pass);
        model.addAttribute("bankAccount", bankAccount);
        model.addAttribute("bankOwner",   bankOwner);
        model.addAttribute("bankName",    bankName);

        // QR VietQR
        String qrUrl = String.format(
            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
            bankName, bankAccount,
            pass.getAmountDue(),
            pass.getPaymentCode(),
            bankOwner.replace(" ", "%20")
        );
        model.addAttribute("qrUrl", qrUrl);

        // Nếu thẻ ACTIVE → Tạo QR code thẻ tháng
        if ("ACTIVE".equals(pass.getStatus())) {
            try {
                String passQR = qrCodeService.generatePassQR(pass);
                model.addAttribute("passQR", passQR);
            } catch (Exception e) {
                System.err.println("Lỗi tạo QR thẻ: " + e.getMessage());
            }
        }

        return "monthly-pass-detail";
    }
}

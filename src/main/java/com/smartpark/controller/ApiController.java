package com.smartpark.controller;

import com.smartpark.dto.request.CheckinRequest;
import com.smartpark.dto.request.CheckoutRequest;
import com.smartpark.dto.request.CreateAccountRequest;
import com.smartpark.dto.request.CreateMonthlyPassRequest;
import com.smartpark.dto.request.LoginRequest;
import com.smartpark.dto.request.UpdateAccountRequest;
import com.smartpark.dto.response.ApiResponse;
import com.smartpark.dto.response.MonthlyPassResponse;
import com.smartpark.dto.request.*;
import com.smartpark.dto.response.ApiResponse;
import com.smartpark.dto.response.MonthlyPassResponse;
import com.smartpark.dto.response.StaffAccountResponse;
import com.smartpark.model.*;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final ParkingService parkingService;
    private final StaffAccountRepository staffRepo;
    private final AccountService accountService;
    private final BookingRepository bookingRepo;
    private final BookingService bookingService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AccountVerificationService verificationService;
    private final MonthlyPassService monthlyPassService;
    private final QRCodeService qrCodeService;

    @Value("${app.bank.account}") private String bankAccount;
    @Value("${app.bank.owner}")   private String bankOwner;
    @Value("${app.bank.name}")    private String bankName;

    public ApiController(ParkingService parkingService,
                         StaffAccountRepository staffRepo,
                         AccountService accountService,
                         BookingRepository bookingRepo,
                         BookingService bookingService,
                         BCryptPasswordEncoder passwordEncoder,
                         AccountVerificationService verificationService,
                         MonthlyPassService monthlyPassService,
                         QRCodeService qrCodeService) {
        this.parkingService  = parkingService;
        this.staffRepo       = staffRepo;
        this.accountService  = accountService;
        this.bookingRepo     = bookingRepo;
        this.bookingService  = bookingService;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
        this.monthlyPassService = monthlyPassService;
        this.qrCodeService = qrCodeService;
    }

    // ── AUTH ──────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<StaffAccountResponse>> login(
            @Valid @RequestBody LoginRequest req) {
        return staffRepo.findByUsername(req.username())
                .filter(acc -> passwordEncoder.matches(req.password(), acc.getPassword()))
                .map(acc -> ResponseEntity.ok(
                        ApiResponse.success("Đăng nhập thành công", StaffAccountResponse.from(acc))))
                .orElse(ResponseEntity.ok(ApiResponse.error("Sai tên đăng nhập hoặc mật khẩu")));
    }

    // ── SLOTS ─────────────────────────────────────────────────────────────────

    @GetMapping("/slots")
    public ApiResponse<List<ParkingSlot>> getAllSlots() {
        return ApiResponse.success(parkingService.getAllSlots());
    }

    @GetMapping("/slots/zone/{zone}")
    public ApiResponse<List<ParkingSlot>> getByZone(@PathVariable String zone) {
        return ApiResponse.success(parkingService.getSlotsByZone(zone));
    }

    @GetMapping("/slots/stats")
    public ApiResponse<?> getStats() {
        return ApiResponse.success(parkingService.getStats());
    }

    @GetMapping("/slots/search")
    public ApiResponse<List<ParkingSlot>> search(@RequestParam String plate) {
        return ApiResponse.success(parkingService.searchByPlate(plate));
    }

    /**
     * Checkin — dùng @Valid + DTO thay vì Map thô.
     * Bean Validation tự validate trước khi vào method.
     */
    @PostMapping("/checkin")
    public ResponseEntity<ApiResponse<ParkingSlot>> checkin(
            @Valid @RequestBody CheckinRequest req) {
        ParkingSlot slot = parkingService.checkin(req.slotId(), req.licensePlate());
        return ResponseEntity.ok(ApiResponse.success("Xe đã vào ô " + slot.getId(), slot));
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<ParkingSlot>> checkout(
            @Valid @RequestBody CheckoutRequest req) {
        ParkingSlot slot = parkingService.checkout(req.slotId());
        return ResponseEntity.ok(ApiResponse.success("Xe đã ra khỏi ô " + slot.getId(), slot));
    }

    // ── BOOKINGS ──────────────────────────────────────────────────────────────

    @GetMapping("/bookings")
    public ApiResponse<List<Booking>> getBookings() {
        return ApiResponse.success(bookingService.getAll());
    }

    @GetMapping("/bookings/{id}")
    public ResponseEntity<ApiResponse<Booking>> getBooking(@PathVariable Long id) {
        return bookingService.findById(id)
                .map(b -> ResponseEntity.ok(ApiResponse.success(b)))
                .orElse(ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy vé #" + id)));
    }

    // ── ACCOUNTS ──────────────────────────────────────────────────────────────

    @GetMapping("/accounts")
    public ApiResponse<List<StaffAccountResponse>> getAccounts() {
        List<StaffAccountResponse> list = staffRepo.findAllByOrderByStaffCodeAsc()
                .stream()
                .map(StaffAccountResponse::from) // Method reference — Java functional style
                .toList();
        return ApiResponse.success(list);
    }

    /**
     * Tạo tài khoản — dùng @Valid DTO, check duplicate username.
     */
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<StaffAccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest req,
            HttpServletRequest httpRequest) {
        if (staffRepo.existsByUsernameIgnoreCase(req.username())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Tên đăng nhập đã tồn tại"));
        }
        
        // Sử dụng accountService để đảm bảo thread-safe và logic tập trung
        StaffAccount saved = accountService.createAccount(
                req.fullName(), req.username(), req.email(), req.password(), req.role()
        );
        
        // ✅ GỬI MAIL XÁC NHẬN (ASYNC)
        if (req.email() != null && !req.email().isBlank()) {
            try {
                String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName()
                               + (httpRequest.getServerPort() != 80 && httpRequest.getServerPort() != 443
                                  ? ":" + httpRequest.getServerPort() : "");
                verificationService.sendVerificationEmailAsync(saved, baseUrl);
                System.out.println("✓ Email xác nhận đang được gửi đến: " + req.email());
            } catch (Exception e) {
                System.err.println("✗ Không gửi được mail: " + e.getMessage());
                e.printStackTrace();
                // Vẫn trả về success vì account đã tạo xong
            }
        }
        
        return ResponseEntity.ok(ApiResponse.success("Đã tạo tài khoản. Email xác nhận đang được gửi.", StaffAccountResponse.from(saved)));
    }

    @PutMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<StaffAccountResponse>> updateAccount(
            @PathVariable Long id, @Valid @RequestBody UpdateAccountRequest req) {
        return staffRepo.findById(id).map(acc -> {
            acc.setFullName(req.fullName());
            acc.setEmail(req.email());
            acc.setRole(req.role());
            acc.setActive(req.active());
            if (req.password() != null && !req.password().isBlank())
                acc.setPassword(passwordEncoder.encode(req.password()));
            return ResponseEntity.ok(
                    ApiResponse.success(StaffAccountResponse.from(staffRepo.save(acc))));
        }).orElse(ResponseEntity.ok(ApiResponse.error("Không tìm thấy tài khoản #" + id)));
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAccount(@PathVariable Long id) {
        if (!staffRepo.existsById(id))
            return ResponseEntity.ok(ApiResponse.error("Không tìm thấy tài khoản #" + id));
        staffRepo.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá tài khoản", null));
    }

    // ── MONTHLY PASSES ────────────────────────────────────────────────────────

    /** Lấy tất cả thẻ tháng */
    @GetMapping("/monthly-passes")
    public ApiResponse<List<MonthlyPassResponse>> getAllPasses() {
        List<MonthlyPassResponse> list = monthlyPassService.getAll()
                .stream().map(MonthlyPassResponse::from).toList();
        return ApiResponse.success(list);
    }

    /** Tạo thẻ tháng mới */
    @PostMapping("/monthly-passes")
    public ResponseEntity<ApiResponse<MonthlyPassResponse>> createPass(
            @Valid @RequestBody CreateMonthlyPassRequest req) {
        MonthlyPass pass = monthlyPassService.create(req);
        return ResponseEntity.ok(ApiResponse.success("Đã tạo thẻ tháng. Vui lòng thanh toán để kích hoạt.", MonthlyPassResponse.from(pass)));
    }

    /** Lấy chi tiết một thẻ */
    @GetMapping("/monthly-passes/{id}")
    public ResponseEntity<ApiResponse<MonthlyPassResponse>> getPass(@PathVariable Long id) {
        return monthlyPassService.findById(id)
                .map(p -> ResponseEntity.ok(ApiResponse.success(MonthlyPassResponse.from(p))))
                .orElse(ResponseEntity.status(404).body(ApiResponse.error("Không tìm thấy thẻ tháng #" + id)));
    }

    /** Tra cứu thẻ theo biển số */
    @GetMapping("/monthly-passes/search")
    public ApiResponse<List<MonthlyPassResponse>> searchPass(@RequestParam String plate) {
        List<MonthlyPassResponse> list = monthlyPassService.findByPlate(plate)
                .stream().map(MonthlyPassResponse::from).toList();
        return ApiResponse.success(list);
    }

    /** Kiểm tra biển số có thẻ tháng hợp lệ không */
    @GetMapping("/monthly-passes/check")
    public ApiResponse<Map<String, Object>> checkPass(@RequestParam String plate) {
        return monthlyPassService.findActivePass(plate)
                .map(p -> ApiResponse.success(Map.<String, Object>of(
                        "hasPass", true,
                        "pass", MonthlyPassResponse.from(p))))
                .orElse(ApiResponse.success(Map.of("hasPass", false)));
    }

    /** Admin kích hoạt thẻ thủ công */
    @PostMapping("/monthly-passes/{id}/activate")
    public ResponseEntity<ApiResponse<MonthlyPassResponse>> activatePass(@PathVariable Long id) {
        try {
            MonthlyPass pass = monthlyPassService.activate(id);
            return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt thẻ tháng", MonthlyPassResponse.from(pass)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /** Admin huỷ thẻ */
    @PostMapping("/monthly-passes/{id}/cancel")
    public ResponseEntity<ApiResponse<MonthlyPassResponse>> cancelPass(@PathVariable Long id) {
        try {
            MonthlyPass pass = monthlyPassService.cancel(id);
            return ResponseEntity.ok(ApiResponse.success("Đã huỷ thẻ tháng", MonthlyPassResponse.from(pass)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /** Gia hạn thẻ */
    @PostMapping("/monthly-passes/{id}/renew")
    public ResponseEntity<ApiResponse<MonthlyPassResponse>> renewPass(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int months) {
        try {
            MonthlyPass pass = monthlyPassService.renew(id, months);
            return ResponseEntity.ok(ApiResponse.success("Đã gia hạn " + months + " tháng. Vui lòng thanh toán để kích hoạt.", MonthlyPassResponse.from(pass)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // ── RESET ─────────────────────────────────────────────────────────────────

    @PostMapping("/reset/slots")
    public ApiResponse<Void> resetSlots() {
        parkingService.resetAllSlots();
        return ApiResponse.success("Đã reset tất cả ô đỗ", null);
    }

    @PostMapping("/reset/all")
    public ApiResponse<Void> resetAll() {
        bookingRepo.deleteAll();
        parkingService.resetAllSlots();
        return ApiResponse.success("Đã reset toàn bộ hệ thống", null);
    }

    // ── SLOTS AVAILABLE (Moved from ParkingSlotController) ────────────────────

    @GetMapping("/slots/available")
    public ApiResponse<Map<String, Object>> getAvailableSlots(
            @RequestParam(required = false) String vehicleType) {
        if (vehicleType != null && !vehicleType.isEmpty()) {
            List<ParkingSlot> slots = parkingService.getAvailableSlots(vehicleType);
            return ApiResponse.success(Map.of(
                "vehicleType", vehicleType,
                "available", slots.size(),
                "slots", slots.stream().map(ParkingSlot::getId).toList()
            ));
        } else {
            List<ParkingSlot> motoSlots = parkingService.getAvailableSlots("xe_may");
            List<ParkingSlot> carSlots = parkingService.getAvailableSlots("o_to");
            
            return ApiResponse.success(Map.of(
                "motorbike", Map.of(
                    "total", parkingService.getSlotsByZone("motorbike").size(),
                    "available", motoSlots.size(),
                    "slots", motoSlots.stream().map(ParkingSlot::getId).toList()
                ),
                "car", Map.of(
                    "total", parkingService.getSlotsByZone("car").size(),
                    "available", carSlots.size(),
                    "slots", carSlots.stream().map(ParkingSlot::getId).toList()
                )
            ));
        }
    }

    // ── CUSTOMER BOOKINGS (Moved from BookingController) ──────────────────────

    @GetMapping("/status/{id}")
    public ApiResponse<String> getBookingStatus(@PathVariable Long id) {
        return ApiResponse.success(bookingService.findById(id)
                .map(Booking::getStatus)
                .orElse("NOT_FOUND"));
    }

    @PostMapping("/bookings/pre-booking")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPreBooking(
            @Valid @RequestBody PreBookingRequest request,
            HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Vui lòng đăng nhập để đặt trước"));
        }

        if (request.bookingDate().isBefore(LocalDate.now())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Ngày đặt không được trong quá khứ"));
        }

        if (request.endTime().isBefore(request.startTime()) ||
            request.endTime().equals(request.startTime())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Giờ kết thúc phải sau giờ bắt đầu"));
        }

        Booking booking = bookingService.createPreBooking(user, request);

        String qrUrl = String.format(
            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%d&addInfo=%s&accountName=%s",
            bankName, bankAccount,
            booking.getAmountDue(),
            booking.getPaymentCode(),
            bankOwner.replace(" ", "%20")
        );

        Map<String, Object> data = Map.of(
            "bookingId", booking.getId(),
            "paymentCode", booking.getPaymentCode(),
            "amountDue", booking.getAmountDue(),
            "status", booking.getStatus(),
            "qrPaymentUrl", qrUrl,
            "bookingDate", booking.getBookingDate().toString(),
            "startTime", booking.getStartTime().toString(),
            "endTime", booking.getEndTime().toString(),
            "slotId", booking.getSlotId() != null ? booking.getSlotId() : ""
        );

        return ResponseEntity.ok(ApiResponse.success("Đặt chỗ thành công", data));
    }

    @GetMapping("/bookings/user/{userId}")
    public ResponseEntity<ApiResponse<List<Booking>>> getUserBookings(
            @PathVariable Long userId,
            HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null || !user.getId().equals(userId)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Không có quyền truy cập"));
        }
        return ResponseEntity.ok(ApiResponse.success(bookingService.getByUser(userId)));
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @PathVariable Long bookingId,
            HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Vui lòng đăng nhập"));
        }

        Booking booking = bookingService.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        if (booking.getUser() == null || !booking.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Bạn chỉ có thể huỷ booking của mình"));
        }

        if (!"PENDING".equals(booking.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Chỉ có thể huỷ booking đang chờ thanh toán"));
        }

        bookingService.cancel(bookingId);

        if (booking.getSlotId() != null) {
            parkingService.checkout(booking.getSlotId());
        }

        return ResponseEntity.ok(ApiResponse.success("Đã huỷ booking thành công", null));
    }
}

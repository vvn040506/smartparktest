package com.smartpark.controller;

import com.smartpark.model.StaffAccount;
import com.smartpark.model.User;
import com.smartpark.repository.BookingRepository;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.repository.UserRepository;
import com.smartpark.service.AccountVerificationService;
import com.smartpark.service.BookingService;
import com.smartpark.service.MonthlyPassService;
import com.smartpark.service.ParkingService;
import com.smartpark.service.ParkingSlotService.SlotStats;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Controller
public class DashboardController {

    private final ParkingService slotService;
    private final StaffAccountRepository staffRepo;
    private final UserRepository userRepo;
    private final BookingService bookingService;
    private final BookingRepository bookingRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AccountVerificationService verificationService;
    private final MonthlyPassService monthlyPassService;

    public DashboardController(ParkingService slotService,
                               StaffAccountRepository staffRepo,
                               UserRepository userRepo,
                               BookingService bookingService,
                               BookingRepository bookingRepo,
                               BCryptPasswordEncoder passwordEncoder,
                               AccountVerificationService verificationService,
                               MonthlyPassService monthlyPassService) {
        this.slotService          = slotService;
        this.staffRepo            = staffRepo;
        this.userRepo             = userRepo;
        this.bookingService       = bookingService;
        this.bookingRepo          = bookingRepo;
        this.passwordEncoder      = passwordEncoder;
        this.verificationService  = verificationService;
        this.monthlyPassService   = monthlyPassService;
    }

    // ── VERIFY ACCOUNT WITH OTP ──────────────────────────────────────────────

    @GetMapping("/verify-account-otp")
    public String verifyAccountOTPPage(Model model) {
        // Email sẽ được truyền qua flash attribute hoặc user tự nhập
        return "verify-account-otp";
    }

    @PostMapping("/verify-account-otp")
    public String doVerifyAccountOTP(@RequestParam String email,
                                      @RequestParam String otpCode,
                                      @RequestParam String password,
                                      @RequestParam String confirmPassword,
                                      RedirectAttributes ra) {
        if (!password.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "Mật khẩu xác nhận không khớp");
            ra.addFlashAttribute("email", email);
            return "redirect:/verify-account-otp";
        }
        
        if (password.length() < 6) {
            ra.addFlashAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự");
            ra.addFlashAttribute("email", email);
            return "redirect:/verify-account-otp";
        }
        
        String result = verificationService.verifyOTPAndSetPassword(email, otpCode, password);
        if ("OK".equals(result)) {
            ra.addFlashAttribute("success", "Xác nhận thành công! Bạn có thể đăng nhập ngay.");
            return "redirect:/login";
        }
        
        ra.addFlashAttribute("error", result);
        ra.addFlashAttribute("email", email);
        return "redirect:/verify-account-otp";
    }

    @GetMapping("/map")
    public String publicMap(HttpSession session, Model model) {
        addStatsToModel(model);
        return "public-map";
    }


    // ── STAFF DASHBOARD ───────────────────────────────────────────────────────

    @GetMapping("/staff")
    public String staffDashboard(HttpSession session, Model model) {
        StaffAccount user = requireLogin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        addStatsToModel(model);
        return "staff-dashboard";
    }

    @PostMapping("/staff/checkin")
    public String checkin(@RequestParam String slotId,
                          @RequestParam String licensePlate,
                          HttpSession session,
                          RedirectAttributes ra) {
        if (requireLogin(session) == null) return "redirect:/login";
        try {
            slotService.checkin(slotId, licensePlate);
            ra.addFlashAttribute("success", "Xe " + licensePlate.toUpperCase() + " đã vào ô " + slotId.toUpperCase());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff";
    }

    @PostMapping("/staff/checkout")
    public String checkout(@RequestParam String slotId,
                           HttpSession session,
                           RedirectAttributes ra) {
        if (requireLogin(session) == null) return "redirect:/login";
        try {
            var slot = slotService.checkout(slotId);
            ra.addFlashAttribute("success", "Xe đã ra khỏi ô " + slot.getId());
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff";
    }

    // ── ADMIN DASHBOARD ───────────────────────────────────────────────────────

    @GetMapping("/admin")
    public String adminDashboard(HttpSession session, Model model) {
        StaffAccount user = requireAdmin(session);
        if (user == null) return "redirect:/login";
        model.addAttribute("user", user);
        addStatsToModel(model);
        List<StaffAccount> accounts = staffRepo.findAllByOrderByStaffCodeAsc();
        model.addAttribute("accounts", accounts);
        model.addAttribute("customers", userRepo.findAll());
        model.addAttribute("bookings", bookingService.getAll());
        var passes = monthlyPassService.getAll();
        model.addAttribute("monthlyPasses", passes);
        model.addAttribute("activePassCount", passes.stream()
                .filter(p -> "ACTIVE".equals(p.getStatus())).count());
        return "admin-dashboard";
    }

    @PostMapping("/admin/monthly-passes/activate")
    public String activatePass(@RequestParam Long id,
                               HttpSession session,
                               RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        try {
            monthlyPassService.activate(id);
            ra.addFlashAttribute("success", "Đã kích hoạt thẻ tháng #" + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/monthly-passes/cancel")
    public String cancelPass(@RequestParam Long id,
                             HttpSession session,
                             RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        try {
            monthlyPassService.cancel(id);
            ra.addFlashAttribute("success", "Đã huỷ thẻ tháng #" + id);
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/monthly-passes/renew")
    public String renewPass(@RequestParam Long id,
                            @RequestParam(defaultValue = "1") int months,
                            HttpSession session,
                            RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        try {
            monthlyPassService.renew(id, months);
            ra.addFlashAttribute("success", "Đã gia hạn thẻ tháng #" + id + " thêm " + months + " tháng. Chờ thanh toán.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    // ── TEST EMAIL ────────────────────────────────────────────────────────────
    
    @GetMapping("/admin/test-email")
    @ResponseBody
    public String testEmail(@RequestParam String email, HttpSession session) {
        if (requireAdmin(session) == null) {
            return "Unauthorized - Admin only";
        }
        
        System.out.println("=== Testing Email Send ===");
        System.out.println("Recipient: " + email);
        
        try {
            // Sinh OTP test
            java.security.SecureRandom random = new java.security.SecureRandom();
            int otp = 100000 + random.nextInt(900000);
            String otpCode = String.valueOf(otp);
            
            // Gửi email test
            verificationService.getEmailService().sendAccountVerificationOTP(
                email, 
                "Test User", 
                "testuser", 
                otpCode
            );
            
            return "✅ Email test đã được gửi đến: " + email + " với OTP: " + otpCode + 
                   "<br><br>Check inbox hoặc spam folder!";
        } catch (Exception e) {
            System.err.println("✗ Test email failed: " + e.getMessage());
            return "❌ Lỗi gửi email: " + e.getMessage() + 
                   "<br><br>Stack trace:<br><pre>" + getStackTraceAsString(e) + "</pre>";
        }
    }
    
    private String getStackTraceAsString(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    @PostMapping("/admin/accounts/add")
    public String addAccount(@RequestParam String fullName,
                             @RequestParam String username,
                             @RequestParam(required = false) String email,
                             @RequestParam String role,
                             HttpSession session,
                             HttpServletRequest request,
                             RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        if (!StringUtils.hasText(fullName) || !StringUtils.hasText(username)) {
            ra.addFlashAttribute("error", "Vui lòng nhập đầy đủ thông tin tài khoản");
            return "redirect:/admin";
        }
        if (staffRepo.existsByUsernameIgnoreCase(username.trim())) {
            ra.addFlashAttribute("error", "Tên đăng nhập đã tồn tại");
            return "redirect:/admin";
        }
        
        String prefix = "admin".equals(role) ? "AD" : "NV";
        long count = staffRepo.countByStaffCodeStartingWith(prefix);
        String code = prefix + String.format("%03d", count + 1);
        
        String emailToUse = StringUtils.hasText(email) ? email.trim().toLowerCase() : null;
        
        if (StringUtils.hasText(email)) {
            // Có email → Yêu cầu xác nhận email trước khi kích hoạt
            StaffAccount acc = new StaffAccount(code, fullName.trim(), username.trim(), emailToUse, "", role);
            acc.setVerified(false); // Chưa xác nhận
            acc.setActive(false);   // Chưa kích hoạt
            staffRepo.save(acc);
            
            // Gửi email xác nhận (async - không block response)
            try {
                String baseUrl = request.getScheme() + "://" + request.getServerName()
                               + (request.getServerPort() != 80 && request.getServerPort() != 443
                                  ? ":" + request.getServerPort() : "");
                verificationService.sendVerificationEmailAsync(acc, baseUrl);
                // Redirect đến trang nhập OTP với email pre-filled
                ra.addFlashAttribute("email", emailToUse);
                ra.addFlashAttribute("success", "Email xác nhận đã được gửi. Vui lòng nhập OTP.");
                return "redirect:/verify-account-otp";
            } catch (Exception e) {
                System.err.println("✗ Không gửi được mail: " + e.getMessage());
                e.printStackTrace();
                ra.addFlashAttribute("error", "Không gửi được email xác nhận");
                return "redirect:/admin";
            }
        } else {
            // Không có email → Tạo với mật khẩu tạm, kích hoạt ngay
            String tempPassword = "changeme123";
            StaffAccount acc = new StaffAccount(code, fullName.trim(), username.trim(), null, 
                                               passwordEncoder.encode(tempPassword), role);
            acc.setVerified(true);
            acc.setActive(true);
            staffRepo.save(acc);
            
            ra.addFlashAttribute("success", "Đã tạo tài khoản " + username.trim() + " với mật khẩu tạm: changeme123");
        }
        
        return "redirect:/admin";
    }

    @PostMapping("/admin/accounts/update")
    public String updateAccount(@RequestParam Long id,
                                @RequestParam String fullName,
                                @RequestParam String username,
                                @RequestParam(required = false) String password,
                                @RequestParam(required = false) String email,
                                @RequestParam String role,
                                HttpSession session,
                                RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        staffRepo.findById(id).ifPresentOrElse(acc -> {
            acc.setFullName(fullName.trim());
            acc.setUsername(username.trim());
            if (StringUtils.hasText(password)) acc.setPassword(passwordEncoder.encode(password));
            if (StringUtils.hasText(email)) acc.setEmail(email.trim().toLowerCase());
            acc.setRole(role);
            staffRepo.save(acc);
            ra.addFlashAttribute("success", "Đã cập nhật tài khoản " + username.trim());
        }, () -> ra.addFlashAttribute("error", "Không tìm thấy tài khoản"));
        return "redirect:/admin";
    }

    @PostMapping("/admin/accounts/delete")
    public String deleteAccount(@RequestParam Long id,
                                HttpSession session,
                                RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        staffRepo.findById(id).ifPresentOrElse(
                acc -> { 
                    // Xóa token xác nhận trước (nếu có)
                    verificationService.deleteTokenByAccount(acc);
                    // Sau đó mới xóa account
                    staffRepo.deleteById(id); 
                    ra.addFlashAttribute("success", "Đã xoá tài khoản " + acc.getUsername()); 
                },
                ()  -> ra.addFlashAttribute("error", "Không tìm thấy tài khoản")
        );
        return "redirect:/admin";
    }

    @PostMapping("/admin/customers/delete")
    @Transactional
    public String deleteCustomer(@RequestParam Long id,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        userRepo.findById(id).ifPresentOrElse(
                u -> {
                    // Xóa các dữ liệu liên quan trước để tránh lỗi Referential integrity
                    verificationService.deleteTokenByUser(u);
                    bookingRepo.deleteByUserId(id);
                    monthlyPassService.deleteByUserId(id);
                    
                    userRepo.deleteById(id);
                    ra.addFlashAttribute("success", "Đã xoá khách hàng " + u.getUsername());
                },
                () -> ra.addFlashAttribute("error", "Không tìm thấy khách hàng")
        );
        return "redirect:/admin";
    }

    @GetMapping("/admin/accounts/verify-status/{id}")
    @ResponseBody
    public java.util.Map<String, String> getVerifyStatus(@PathVariable Long id, HttpSession session) {
        if (requireAdmin(session) == null) {
            return java.util.Map.of("status", "unauthorized");
        }
        
        return staffRepo.findById(id)
            .map(acc -> {
                if (acc.isVerified() && acc.isActive()) {
                    return java.util.Map.of("status", "verified");
                } else if (verificationService.isTokenExpired(acc)) {
                    return java.util.Map.of("status", "expired");
                } else {
                    return java.util.Map.of("status", "pending");
                }
            })
            .orElse(java.util.Map.of("status", "not_found"));
    }

    @PostMapping("/admin/reset/slots")
    public String resetSlots(HttpSession session, RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        slotService.resetAllSlots();
        ra.addFlashAttribute("success", "Đã reset tất cả ô đỗ về trạng thái trống");
        return "redirect:/admin";
    }

    @PostMapping("/admin/reset/all")
    public String resetAll(HttpSession session, RedirectAttributes ra) {
        if (requireAdmin(session) == null) return "redirect:/login";
        bookingRepo.deleteAll();
        slotService.resetAllSlots();
        ra.addFlashAttribute("success", "Đã reset toàn bộ hệ thống");
        return "redirect:/admin";
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private void addStatsToModel(Model model) {
        SlotStats s = slotService.getStats();
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
        model.addAttribute("motoSlots",  slotService.getSlotsByZone("motorbike"));
        model.addAttribute("carSlots",   slotService.getSlotsByZone("car"));
        model.addAttribute("motoRows",   List.of("A","B","C","D","E"));
        model.addAttribute("carRows",    List.of("A","B","C"));
    }

    private StaffAccount requireLogin(HttpSession session) {
        return (StaffAccount) session.getAttribute("user");
    }

    private StaffAccount requireAdmin(HttpSession session) {
        StaffAccount u = requireLogin(session);
        return (u != null && "admin".equals(u.getRole())) ? u : null;
    }
}

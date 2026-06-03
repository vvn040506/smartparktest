package com.smartpark.controller;

import com.smartpark.model.StaffAccount;
import com.smartpark.model.User;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.service.AccountService;
import com.smartpark.service.AccountVerificationService;
import com.smartpark.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;
    private final AccountService accountService;
    private final StaffAccountRepository staffRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AccountVerificationService verificationService;

    public AuthController(UserService userService,
                          AccountService accountService,
                          StaffAccountRepository staffRepo,
                          BCryptPasswordEncoder passwordEncoder,
                          AccountVerificationService verificationService) {
        this.userService = userService;
        this.accountService = accountService;
        this.staffRepo = staffRepo;
        this.passwordEncoder = passwordEncoder;
        this.verificationService = verificationService;
    }

    // ── LOGIN ──────────────────────────────────────────
    @GetMapping("/login")
    public String loginPage() { return "login"; }

    @PostMapping("/login")
    public String doLogin(@RequestParam String username,
                          @RequestParam String password,
                          HttpSession session, Model model) {
        // Thử đăng nhập Staff Account trước
        StaffAccount staff = staffRepo.findByUsername(username.trim())
                .filter(acc -> passwordEncoder.matches(password, acc.getPassword()))
                .filter(StaffAccount::isActive) // Chỉ cho phép tài khoản đã kích hoạt
                .orElse(null);
        
        if (staff != null) {
            session.setAttribute("user", staff);
            
            // Manually set authentication in SecurityContext
            String role = "ROLE_" + staff.getRole().toUpperCase();
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(role));
            Authentication auth = new UsernamePasswordAuthenticationToken(staff.getUsername(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            return "admin".equals(staff.getRole()) ? "redirect:/admin" : "redirect:/staff";
        }
        
        // Nếu không phải staff, thử User (khách hàng)
        User user = userService.login(username, password);
        if (user != null) {
            session.setAttribute("currentUser", user);
            
            // Manually set authentication in SecurityContext for User
            List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
            Authentication auth = new UsernamePasswordAuthenticationToken(user.getUsername(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            
            return "redirect:/booking";
        }
        
        model.addAttribute("error", "Sai tên đăng nhập hoặc mật khẩu");
        return "login";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    // ── REGISTER ───────────────────────────────────────
    @GetMapping("/register")
    public String registerPage() { return "register"; }

    @PostMapping("/register")
    public String doRegister(@RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String password,
                             Model model) {
        boolean ok = userService.register(username, email, password);
        if (!ok) {
            model.addAttribute("error", "Username hoặc email đã tồn tại");
            return "register";
        }
        model.addAttribute("email", email);
        model.addAttribute("success", "Mã OTP đã được gửi đến email của bạn.");
        return "verify-user-otp";
    }

    @GetMapping("/verify-user-otp")
    public String verifyUserOTPPage(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        return "verify-user-otp";
    }

    @PostMapping("/verify-user-otp")
    public String doVerifyUserOTP(@RequestParam String email,
                                  @RequestParam String otpCode,
                                  Model model) {
        String result = verificationService.verifyUserOTP(email, otpCode);
        if ("OK".equals(result)) {
            return "redirect:/login?verified=true";
        }
        model.addAttribute("error", result);
        model.addAttribute("email", email);
        return "verify-user-otp";
    }

    // ── FORGOT PASSWORD (User) ────────────────────────────────
    @GetMapping("/forgot-password")
    public String forgotPage() { return "forgot-password"; }

    @PostMapping("/forgot-password")
    public String doForgot(@RequestParam String email,
                           RedirectAttributes redirectAttributes) {
        userService.sendResetOTP(email);
        // Luôn hiện thông báo thành công (tránh lộ email có tồn tại không)
        redirectAttributes.addFlashAttribute("success",
            "Nếu email tồn tại, mã OTP đặt lại mật khẩu đã được gửi đến email của bạn.");
        return "redirect:/reset-password?email=" + email;
    }

    // ── RESET PASSWORD (User) ─────────────────────────────────
    @GetMapping("/reset-password")
    public String resetPage() {
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String doReset(@RequestParam String email,
                          @RequestParam String otpCode,
                          @RequestParam String newPassword,
                          @RequestParam String confirmPassword,
                          Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            return "reset-password";
        }
        
        if (newPassword.length() < 6) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự");
            return "reset-password";
        }

        String result = userService.verifyOTPAndReset(email, otpCode, newPassword);
        if ("OK".equals(result)) return "redirect:/login?reset=true";
        
        model.addAttribute("error", result);
        model.addAttribute("email", email);
        return "reset-password";
    }

    // ── FORGOT PASSWORD (Staff) ────────────────────────────────
    @GetMapping("/staff/forgot-password")
    public String staffForgotPage() { return "staff-forgot-password"; }

    @PostMapping("/staff/forgot-password")
    public String doStaffForgot(@RequestParam String email,
                                HttpServletRequest request,
                                RedirectAttributes redirectAttributes) {
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        accountService.sendStaffResetLink(email, baseUrl);
        // Luôn hiện thông báo thành công (tránh lộ email có tồn tại không)
        redirectAttributes.addFlashAttribute("success",
            "Nếu email tồn tại, mã OTP đã được gửi đến email của bạn. Mã có hiệu lực trong 10 phút.");
        return "redirect:/staff/reset-password?email=" + email;
    }

    // ── RESET PASSWORD (Staff) với OTP ─────────────────────────────────
    @GetMapping("/staff/reset-password")
    public String staffResetPage() {
        // Không cần token nữa, chỉ cần form nhập email + OTP + password mới
        return "staff-reset-password";
    }

    @PostMapping("/staff/reset-password")
    public String doStaffReset(@RequestParam String email,
                               @RequestParam String otpCode,
                               @RequestParam String newPassword,
                               @RequestParam String confirmPassword,
                               Model model) {
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            return "staff-reset-password";
        }
        
        if (newPassword.length() < 6) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự");
            return "staff-reset-password";
        }
        
        String result = accountService.verifyOTPAndReset(email, otpCode, newPassword);
        if ("OK".equals(result)) {
            return "redirect:/login?reset=true";
        }
        
        model.addAttribute("error", result);
        model.addAttribute("email", email);
        return "staff-reset-password";
    }
}
package com.smartpark.service;

import com.smartpark.model.PasswordResetToken;
import com.smartpark.model.User;
import com.smartpark.repository.PasswordResetTokenRepository;
import com.smartpark.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
public class UserService {

    @Autowired private UserRepository userRepo;
    @Autowired private PasswordResetTokenRepository tokenRepo;
    @Autowired private EmailService emailService;
    @Autowired private BCryptPasswordEncoder encoder; // FIX: Inject instead of creating new instance
    @Autowired private AccountVerificationService verificationService;

    // Đăng ký
    public boolean register(String username, String email, String rawPassword) {
        if (userRepo.existsByEmail(email) || userRepo.existsByUsername(username)) return false;
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setPassword(encoder.encode(rawPassword));
        u.setVerified(false);
        u.setActive(false);
        userRepo.save(u);
        
        // Gửi OTP xác nhận
        verificationService.sendUserVerificationEmailAsync(u);
        
        return true;
    }

    // Đăng nhập
    public User login(String username, String rawPassword) {
        return userRepo.findByUsername(username)
                .filter(u -> encoder.matches(rawPassword, u.getPassword()))
                .filter(User::isActive) // Chỉ cho phép user đã kích hoạt
                .orElse(null);
    }

    // Gửi OTP reset mật khẩu
    @Transactional
    public boolean sendResetOTP(String email) {
        return userRepo.findByEmail(email).map(user -> {
            // Xoá token cũ nếu có
            tokenRepo.deleteByUser(user);

            // Tạo mã OTP 6 số
            String otp = String.format("%06d", new Random().nextInt(1000000));
            
            PasswordResetToken prt = new PasswordResetToken();
            prt.setToken(otp);
            prt.setUser(user);
            prt.setExpiryDate(LocalDateTime.now().plusMinutes(10)); // OTP hết hạn sau 10 phút
            tokenRepo.save(prt);

            emailService.sendUserResetOTP(email, user.getUsername(), otp);
            return true;
        }).orElse(false);
    }

    // Đặt lại mật khẩu bằng OTP
    @Transactional
    public String verifyOTPAndReset(String email, String otpCode, String newPassword) {
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return "Email không tồn tại";

        PasswordResetToken prt = tokenRepo.findByToken(otpCode).orElse(null);
        if (prt == null || !prt.getUser().getId().equals(user.getId())) {
            return "Mã OTP không hợp lệ";
        }
        
        if (prt.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "Mã OTP đã hết hạn";
        }

        user.setPassword(encoder.encode(newPassword));
        userRepo.save(user);
        tokenRepo.delete(prt);
        return "OK";
    }
}
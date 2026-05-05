package com.smartpark.service;

import com.smartpark.model.AccountVerificationToken;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.AccountVerificationTokenRepository;
import com.smartpark.repository.StaffAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class AccountVerificationService {

    @Autowired private AccountVerificationTokenRepository tokenRepo;
    @Autowired private StaffAccountRepository staffRepo;
    @Autowired private EmailService emailService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    public EmailService getEmailService() {
        return emailService;
    }

    @Async
    public void sendVerificationEmailAsync(StaffAccount account, String baseUrl) {
        try {
            String otpCode = createVerificationOTP(account);
            emailService.sendAccountVerificationOTP(account.getEmail(), account.getFullName(), account.getUsername(), otpCode);
            System.out.println("✓ Email xác nhận với OTP đã được gửi đến: " + account.getEmail());
        } catch (Exception e) {
            System.err.println("✗ Lỗi gửi email xác nhận: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendVerificationEmail(StaffAccount account, String baseUrl) {
        try {
            String token = createVerificationToken(account);
            String verifyLink = baseUrl + "/verify-account?token=" + token;
            emailService.sendAccountVerificationEmail(account.getEmail(), verifyLink, account.getUsername());
            System.out.println("✓ Email xác nhận đã được gửi đến: " + account.getEmail());
        } catch (Exception e) {
            System.err.println("✗ Lỗi gửi email xác nhận: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Transactional
    public String createVerificationOTP(StaffAccount account) {
        tokenRepo.deleteByStaffAccount(account);
        tokenRepo.flush(); // ← FIX: flush để tránh duplicate key

        String otpCode = generateOTP();
        String token = UUID.randomUUID().toString();
        AccountVerificationToken vt = new AccountVerificationToken();
        vt.setToken(token);
        vt.setStaffAccount(account);
        vt.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        vt.setOtpCode(otpCode);
        vt.setOtpAttempts(0);
        vt.setOtpUsed(false);
        tokenRepo.save(vt);

        return otpCode;
    }

    private String generateOTP() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    @Transactional
    public String createVerificationToken(StaffAccount account) {
        tokenRepo.deleteByStaffAccount(account);
        tokenRepo.flush(); // ← FIX: flush để tránh duplicate key

        String token = UUID.randomUUID().toString();
        AccountVerificationToken vt = new AccountVerificationToken();
        vt.setToken(token);
        vt.setStaffAccount(account);
        vt.setExpiryDate(LocalDateTime.now().plusMinutes(5));
        tokenRepo.save(vt);

        return token;
    }

    @Transactional
    public String verifyOTPAndSetPassword(String email, String otpCode, String newPassword) {
        StaffAccount account = staffRepo.findByEmail(email).orElse(null);
        if (account == null) return "Email không tồn tại";

        AccountVerificationToken vt = tokenRepo.findByStaffAccount(account).orElse(null);
        if (vt == null) return "Không tìm thấy OTP. Vui lòng yêu cầu gửi lại.";

        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepo.delete(vt);
            if (!account.isVerified()) staffRepo.delete(account);
            return "OTP đã hết hạn";
        }

        if (vt.isOtpUsed()) return "OTP đã được sử dụng";

        if (vt.getOtpAttempts() >= 5) {
            tokenRepo.delete(vt);
            if (!account.isVerified()) staffRepo.delete(account);
            return "Đã nhập sai quá 5 lần. Vui lòng yêu cầu OTP mới.";
        }

        if (!otpCode.equals(vt.getOtpCode())) {
            vt.setOtpAttempts(vt.getOtpAttempts() + 1);
            tokenRepo.save(vt);
            return "OTP không đúng. Còn " + (5 - vt.getOtpAttempts()) + " lần thử.";
        }

        account.setPassword(passwordEncoder.encode(newPassword));
        account.setVerified(true);
        account.setActive(true);
        staffRepo.save(account);

        vt.setOtpUsed(true);
        tokenRepo.save(vt);

        return "OK";
    }

    @Transactional
    public String verifyAccount(String token) {
        AccountVerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null) return "Token không hợp lệ";
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            StaffAccount account = vt.getStaffAccount();
            tokenRepo.delete(vt);
            if (!account.isVerified()) staffRepo.delete(account);
            return "Token đã hết hạn";
        }
        return "OK";
    }

    public String validateToken(String token) {
        AccountVerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null) return "Token không hợp lệ";
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) return "Token đã hết hạn";
        return "OK";
    }

    @Transactional
    public String setPassword(String token, String newPassword) {
        AccountVerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null) return "Token không hợp lệ";
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) return "Token đã hết hạn";

        StaffAccount account = vt.getStaffAccount();
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setVerified(true);
        account.setActive(true);
        staffRepo.save(account);
        tokenRepo.delete(vt);

        return "OK";
    }

    public boolean isTokenExpired(StaffAccount account) {
        return tokenRepo.findByStaffAccount(account)
                .map(token -> token.getExpiryDate().isBefore(LocalDateTime.now()))
                .orElse(true);
    }

    @Transactional
    public void deleteTokenByAccount(StaffAccount account) {
        tokenRepo.deleteByStaffAccount(account);
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<AccountVerificationToken> expiredTokens = tokenRepo.findByExpiryDateBefore(now);
            for (AccountVerificationToken token : expiredTokens) {
                try {
                    StaffAccount account = token.getStaffAccount();
                    tokenRepo.delete(token);
                    if (!account.isVerified()) staffRepo.delete(account);
                } catch (Exception e) {
                    System.err.println("Error cleaning up token: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in cleanupExpiredTokens: " + e.getMessage());
        }
    }
}
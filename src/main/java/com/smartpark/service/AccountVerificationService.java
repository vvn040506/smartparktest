package com.smartpark.service;

import com.smartpark.model.AccountVerificationToken;
import com.smartpark.model.StaffAccount;
import com.smartpark.model.User;
import com.smartpark.repository.AccountVerificationTokenRepository;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.repository.UserRepository;
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
    @Autowired private UserRepository userRepo;
    @Autowired private EmailService emailService;
    @Autowired private BCryptPasswordEncoder passwordEncoder;

    public EmailService getEmailService() {
        return emailService;
    }

    @Async
    @Transactional
    public void sendVerificationEmailAsync(StaffAccount account, String baseUrl) {
        try {
            // Reload account to ensure it's attached
            StaffAccount attachedAccount = staffRepo.findById(account.getId()).orElse(account);
            String otpCode = createVerificationOTP(attachedAccount);
            emailService.sendAccountVerificationOTP(attachedAccount.getEmail(), attachedAccount.getFullName(), attachedAccount.getUsername(), otpCode);
            System.out.println("✓ Email xác nhận với OTP đã được gửi đến: " + attachedAccount.getEmail());
        } catch (Exception e) {
            System.err.println("✗ Lỗi gửi email xác nhận: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    @Transactional
    public void sendUserVerificationEmailAsync(User user) {
        try {
            // Reload user to ensure it's attached to the current persistence context
            User attachedUser = userRepo.findById(user.getId()).orElse(user);
            String otpCode = createUserVerificationOTP(attachedUser);
            emailService.sendAccountVerificationOTP(attachedUser.getEmail(), attachedUser.getUsername(), attachedUser.getUsername(), otpCode);
            System.out.println("✓ Email xác nhận với OTP đã được gửi đến khách hàng: " + attachedUser.getEmail());
        } catch (Exception e) {
            System.err.println("✗ Lỗi gửi email xác nhận khách hàng: " + e.getMessage());
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

    @Transactional
    public String createUserVerificationOTP(User user) {
        tokenRepo.deleteByUser(user);
        tokenRepo.flush();

        String otpCode = generateOTP();
        String token = UUID.randomUUID().toString();
        AccountVerificationToken vt = new AccountVerificationToken();
        vt.setToken(token);
        vt.setUser(user);
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
    public String verifyUserOTP(String email, String otpCode) {
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return "Email không tồn tại";

        AccountVerificationToken vt = tokenRepo.findByUser(user).orElse(null);
        if (vt == null) return "Không tìm thấy OTP. Vui lòng yêu cầu gửi lại.";

        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepo.delete(vt);
            if (!user.isVerified()) userRepo.delete(user);
            return "OTP đã hết hạn";
        }

        if (vt.isOtpUsed()) return "OTP đã được sử dụng";

        if (vt.getOtpAttempts() >= 5) {
            tokenRepo.delete(vt);
            if (!user.isVerified()) userRepo.delete(user);
            return "Đã nhập sai quá 5 lần. Vui lòng yêu cầu OTP mới.";
        }

        if (!otpCode.equals(vt.getOtpCode())) {
            vt.setOtpAttempts(vt.getOtpAttempts() + 1);
            tokenRepo.save(vt);
            return "OTP không đúng. Còn " + (5 - vt.getOtpAttempts()) + " lần thử.";
        }

        user.setVerified(true);
        user.setActive(true);
        userRepo.save(user);

        vt.setOtpUsed(true);
        tokenRepo.save(vt);

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

    @Transactional
    public void deleteTokenByUser(User user) {
        tokenRepo.deleteByUser(user);
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
                    User user = token.getUser();
                    tokenRepo.delete(token);
                    if (account != null && !account.isVerified()) staffRepo.delete(account);
                    if (user != null && !user.isVerified()) userRepo.delete(user);
                } catch (Exception e) {
                    System.err.println("Error cleaning up token: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Error in cleanupExpiredTokens: " + e.getMessage());
        }
    }
}
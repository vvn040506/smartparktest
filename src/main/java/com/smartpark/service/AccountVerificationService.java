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

    // Getter for testing
    public EmailService getEmailService() {
        return emailService;
    }

    /**
     * Gửi email xác nhận cho tài khoản mới với OTP (không gửi link)
     * KHÔNG có @Transactional để tránh block khi gửi email
     * Sử dụng @Async để gửi email bất đồng bộ
     */
    @Async
    public void sendVerificationEmailAsync(StaffAccount account, String baseUrl) {
        try {
            // Tạo và lưu OTP trong transaction riêng
            String otpCode = createVerificationOTP(account);
            
            // Gửi email với OTP (không gửi link)
            emailService.sendAccountVerificationOTP(account.getEmail(), account.getFullName(), account.getUsername(), otpCode);
            System.out.println("✓ Email xác nhận với OTP đã được gửi đến: " + account.getEmail());
        } catch (Exception e) {
            System.err.println("✗ Lỗi gửi email xác nhận: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Gửi email xác nhận (synchronous version - for backward compatibility)
     */
    public void sendVerificationEmail(StaffAccount account, String baseUrl) {
        try {
            // Tạo và lưu token trong transaction riêng
            String token = createVerificationToken(account);
            
            // Gửi email (không trong transaction)
            String verifyLink = baseUrl + "/verify-account?token=" + token;
            emailService.sendAccountVerificationEmail(account.getEmail(), verifyLink, account.getUsername());
            System.out.println("✓ Email xác nhận đã được gửi đến: " + account.getEmail());
        } catch (Exception e) {
            System.err.println("✗ Lỗi gửi email xác nhận: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Tạo OTP xác nhận trong transaction riêng (cho account verification)
     */
    @Transactional
    public String createVerificationOTP(StaffAccount account) {
        // Xóa token/OTP cũ nếu có
        tokenRepo.deleteByStaffAccount(account);

        // Sinh OTP 6 chữ số
        String otpCode = generateOTP();
        
        // Tạo token mới với OTP
        String token = UUID.randomUUID().toString(); // Token để tracking, không gửi cho user
        AccountVerificationToken vt = new AccountVerificationToken();
        vt.setToken(token);
        vt.setStaffAccount(account);
        vt.setExpiryDate(LocalDateTime.now().plusMinutes(10)); // OTP hết hạn sau 10 phút
        vt.setOtpCode(otpCode);
        vt.setOtpAttempts(0);
        vt.setOtpUsed(false);
        tokenRepo.save(vt);
        
        return otpCode;
    }
    
    /**
     * Sinh OTP 6 chữ số ngẫu nhiên
     */
    private String generateOTP() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        int otp = 100000 + random.nextInt(900000); // 100000 - 999999
        return String.valueOf(otp);
    }

    /**
     * Tạo token xác nhận trong transaction riêng
     */
    @Transactional
    public String createVerificationToken(StaffAccount account) {
        // Xóa token cũ nếu có
        tokenRepo.deleteByStaffAccount(account);

        // Tạo token mới
        String token = UUID.randomUUID().toString();
        AccountVerificationToken vt = new AccountVerificationToken();
        vt.setToken(token);
        vt.setStaffAccount(account);
        vt.setExpiryDate(LocalDateTime.now().plusMinutes(5)); // Hết hạn sau 5 phút
        tokenRepo.save(vt);
        
        return token;
    }

    /**
     * Verify OTP và đặt mật khẩu cho tài khoản mới
     */
    @Transactional
    public String verifyOTPAndSetPassword(String email, String otpCode, String newPassword) {
        // Tìm account theo email
        StaffAccount account = staffRepo.findByEmail(email).orElse(null);
        if (account == null) {
            return "Email không tồn tại";
        }
        
        // Tìm token theo account
        AccountVerificationToken vt = tokenRepo.findByStaffAccount(account).orElse(null);
        if (vt == null) {
            return "Không tìm thấy OTP. Vui lòng yêu cầu gửi lại.";
        }
        
        // Check hết hạn
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            tokenRepo.delete(vt);
            // Xóa account chưa xác minh
            if (!account.isVerified()) {
                staffRepo.delete(account);
            }
            return "OTP đã hết hạn";
        }
        
        // Check đã dùng chưa
        if (vt.isOtpUsed()) {
            return "OTP đã được sử dụng";
        }
        
        // Check số lần nhập sai
        if (vt.getOtpAttempts() >= 5) {
            tokenRepo.delete(vt);
            // Xóa account chưa xác minh
            if (!account.isVerified()) {
                staffRepo.delete(account);
            }
            return "Đã nhập sai quá 5 lần. Vui lòng yêu cầu OTP mới.";
        }
        
        // Check OTP đúng không
        if (!otpCode.equals(vt.getOtpCode())) {
            vt.setOtpAttempts(vt.getOtpAttempts() + 1);
            tokenRepo.save(vt);
            return "OTP không đúng. Còn " + (5 - vt.getOtpAttempts()) + " lần thử.";
        }
        
        // OTP đúng → Set password và kích hoạt account
        account.setPassword(passwordEncoder.encode(newPassword));
        account.setVerified(true);
        account.setActive(true);
        staffRepo.save(account);
        
        // Đánh dấu OTP đã dùng
        vt.setOtpUsed(true);
        tokenRepo.save(vt);
        
        return "OK";
    }

    /**
     * Xác nhận token hợp lệ (không kích hoạt tài khoản ngay)
     */
    @Transactional
    public String verifyAccount(String token) {
        AccountVerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null) return "Token không hợp lệ";
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            // Token hết hạn - xóa token và tài khoản chưa xác minh
            StaffAccount account = vt.getStaffAccount();
            tokenRepo.delete(vt);
            if (!account.isVerified()) {
                staffRepo.delete(account);
            }
            return "Token đã hết hạn";
        }
        
        return "OK";
    }

    /**
     * Validate token (dùng cho GET /set-password)
     */
    public String validateToken(String token) {
        AccountVerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null) return "Token không hợp lệ";
        if (vt.getExpiryDate().isBefore(LocalDateTime.now())) {
            return "Token đã hết hạn";
        }
        return "OK";
    }

    /**
     * Đặt mật khẩu và kích hoạt tài khoản
     */
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

    /**
     * Kiểm tra token có hết hạn không
     */
    public boolean isTokenExpired(StaffAccount account) {
        return tokenRepo.findByStaffAccount(account)
            .map(token -> token.getExpiryDate().isBefore(LocalDateTime.now()))
            .orElse(true);
    }

    /**
     * Xóa token theo account (dùng khi xóa account)
     */
    @Transactional
    public void deleteTokenByAccount(StaffAccount account) {
        tokenRepo.deleteByStaffAccount(account);
    }

    /**
     * Scheduled job: Xóa tokens hết hạn và tài khoản chưa xác minh
     * Chạy mỗi 5 phút
     */
    @Scheduled(fixedRate = 300000) // 5 phút = 300,000 ms
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<AccountVerificationToken> expiredTokens = tokenRepo.findByExpiryDateBefore(now);
            
            for (AccountVerificationToken token : expiredTokens) {
                try {
                    StaffAccount account = token.getStaffAccount();
                    tokenRepo.delete(token);
                    
                    // Xóa tài khoản nếu chưa được xác minh
                    if (!account.isVerified()) {
                        staffRepo.delete(account);
                    }
                } catch (Exception e) {
                    // Log lỗi nhưng tiếp tục xử lý các token khác
                    System.err.println("Error cleaning up token: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // Log lỗi tổng thể nhưng không throw exception để không ảnh hưởng connection pool
            System.err.println("Error in cleanupExpiredTokens: " + e.getMessage());
        }
    }
}

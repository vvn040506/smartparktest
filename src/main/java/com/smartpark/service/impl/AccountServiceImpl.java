package com.smartpark.service.impl;

import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.model.PasswordResetToken;
import com.smartpark.model.StaffAccount;
import com.smartpark.repository.PasswordResetTokenRepository;
import com.smartpark.repository.StaffAccountRepository;
import com.smartpark.service.AccountService;
import com.smartpark.service.EmailService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountServiceImpl implements AccountService {

    private final StaffAccountRepository staffRepo;
    private final BCryptPasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository tokenRepo;
    private final EmailService emailService;
    private final com.smartpark.repository.AccountVerificationTokenRepository verificationTokenRepo;

    public AccountServiceImpl(StaffAccountRepository staffRepo, 
                             BCryptPasswordEncoder passwordEncoder,
                             PasswordResetTokenRepository tokenRepo,
                             EmailService emailService,
                             com.smartpark.repository.AccountVerificationTokenRepository verificationTokenRepo) {
        this.staffRepo       = staffRepo;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepo       = tokenRepo;
        this.emailService    = emailService;
        this.verificationTokenRepo = verificationTokenRepo;
    }

    @Override
    public Optional<StaffAccount> authenticate(String username, String password) {
        return staffRepo.findByUsername(username)
                .filter(acc -> passwordEncoder.matches(password, acc.getPassword()));
    }

    @Override
    public List<StaffAccount> getAllAccounts() {
        return staffRepo.findAllByOrderByStaffCodeAsc();
    }

    @Override
    public StaffAccount createAccount(String fullName, String username,
                                       String password, String role) {
        long count = staffRepo.count();
        String prefix = "admin".equals(role) ? "AD" : "NV";
        StaffAccount acc = new StaffAccount(
                prefix + String.format("%03d", count + 1),
                fullName, username, null, passwordEncoder.encode(password), role
        );
        // Không auto-verify - yêu cầu xác nhận email
        acc.setVerified(false);
        acc.setActive(false);
        return staffRepo.save(acc);
    }

    @Override
    public StaffAccount updateAccount(Long id, String fullName, String role,
                                       boolean active, String password) {
        StaffAccount acc = staffRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", "id", id));
        acc.setFullName(fullName);
        acc.setRole(role);
        acc.setActive(active);
        if (password != null && !password.isBlank()) {
            acc.setPassword(passwordEncoder.encode(password));
        }
        return staffRepo.save(acc);
    }

    @Override
    public void deleteAccount(Long id) {
        staffRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản", "id", id));
        staffRepo.deleteById(id);
    }

    @Override
    @Transactional
    public boolean sendStaffResetLink(String email, String baseUrl) {
        Optional<StaffAccount> accountOpt = staffRepo.findByEmail(email);
        if (accountOpt.isEmpty()) {
            return false;
        }
        
        StaffAccount account = accountOpt.get();
        
        // Xóa token/OTP cũ nếu có
        verificationTokenRepo.deleteByStaffAccount(account);
        verificationTokenRepo.flush();
        
        // Sinh OTP 6 chữ số
        String otpCode = generateOTP();
        
        // Tạo token mới với OTP
        String token = UUID.randomUUID().toString(); // Token để tracking, không gửi cho user
        com.smartpark.model.AccountVerificationToken resetToken = new com.smartpark.model.AccountVerificationToken();
        resetToken.setToken(token);
        resetToken.setStaffAccount(account);
        resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(10)); // OTP hết hạn sau 10 phút
        resetToken.setOtpCode(otpCode);
        resetToken.setOtpAttempts(0);
        resetToken.setOtpUsed(false);
        verificationTokenRepo.save(resetToken);
        
        // Gửi email với OTP (không gửi link)
        emailService.sendStaffResetOTP(email, account.getFullName(), otpCode);
        
        return true;
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
     * Verify OTP và cho phép reset password
     */
    @Transactional
    public String verifyOTPAndReset(String email, String otpCode, String newPassword) {
        Optional<StaffAccount> accountOpt = staffRepo.findByEmail(email);
        if (accountOpt.isEmpty()) {
            return "Email không tồn tại";
        }
        
        StaffAccount account = accountOpt.get();
        com.smartpark.model.AccountVerificationToken resetToken = verificationTokenRepo.findByStaffAccount(account).orElse(null);
        
        if (resetToken == null) {
            return "Không tìm thấy OTP. Vui lòng yêu cầu gửi lại.";
        }
        
        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            verificationTokenRepo.delete(resetToken);
            return "OTP đã hết hạn";
        }
        
        if (resetToken.isOtpUsed()) {
            return "OTP đã được sử dụng";
        }
        
        if (resetToken.getOtpAttempts() >= 5) {
            verificationTokenRepo.delete(resetToken);
            return "Đã nhập sai quá 5 lần. Vui lòng yêu cầu OTP mới.";
        }
        
        if (!otpCode.equals(resetToken.getOtpCode())) {
            resetToken.setOtpAttempts(resetToken.getOtpAttempts() + 1);
            verificationTokenRepo.save(resetToken);
            return "OTP không đúng. Còn " + (5 - resetToken.getOtpAttempts()) + " lần thử.";
        }
        
        // OTP đúng → Reset password
        account.setPassword(passwordEncoder.encode(newPassword));
        staffRepo.save(account);
        
        resetToken.setOtpUsed(true);
        verificationTokenRepo.save(resetToken);
        
        return "OK";
    }
}

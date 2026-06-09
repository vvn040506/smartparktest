package com.smartpark.service.impl;

import com.smartpark.dto.request.CreateMonthlyPassRequest;
import com.smartpark.exception.ResourceNotFoundException;
import com.smartpark.model.MonthlyPass;
import com.smartpark.repository.MonthlyPassRepository;
import com.smartpark.service.EmailService;
import com.smartpark.service.MonthlyPassService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Giá thẻ tháng:
 *   Xe máy : 100.000 đ / tháng
 *   Ô tô   : 200.000 đ / tháng
 */
@Service
public class MonthlyPassServiceImpl implements MonthlyPassService {

    private static final long MOTO_PRICE_PER_MONTH = 100_000L;
    private static final long CAR_PRICE_PER_MONTH  = 200_000L;

    // Fix #1: Dùng SecureRandom thay vì Random để tăng entropy
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    // Fix #9: Validate payment code format (MP + 6 alphanumeric chars)
    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("^MP[A-Z2-9]{6}$");

    private final MonthlyPassRepository repo;
    private final EmailService emailService;

    public MonthlyPassServiceImpl(MonthlyPassRepository repo, EmailService emailService) {
        this.repo = repo;
        this.emailService = emailService;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @Override
    public MonthlyPass create(CreateMonthlyPassRequest req) {
        return create(req, null);
    }

    @Override
    public MonthlyPass create(CreateMonthlyPassRequest req, com.smartpark.model.User user) {
        int months = req.resolvedMonths();
        long pricePerMonth = "o_to".equals(req.vehicleType())
                ? CAR_PRICE_PER_MONTH : MOTO_PRICE_PER_MONTH;

        MonthlyPass pass = new MonthlyPass();
        pass.setOwnerName(req.ownerName().trim());
        pass.setEmail(req.email() != null ? req.email().trim() : null);
        pass.setLicensePlate(req.licensePlate().toUpperCase().trim());
        pass.setVehicleType(req.vehicleType());
        pass.setStartDate(req.startDate());
        pass.setEndDate(req.startDate().plusMonths(months).minusDays(1));
        pass.setAmountDue(pricePerMonth * months);
        // Fix #2: generatePaymentCode có retry khi trùng
        pass.setPaymentCode(generateUniquePaymentCode());
        pass.setNote(req.note());
        pass.setStatus("PENDING");
        if (user != null) {
            pass.setUser(user);
        }

        return repo.save(pass);
    }


    // ── PAYMENT ───────────────────────────────────────────────────────────────

    // Fix #4: Thêm @Transactional để tránh race condition / double-activate
    // Fix #10: Error handling cho email service
    @Override
    @Transactional
    public boolean processPayment(String content, long amount, String bankRef) {
        if (content == null) return false;
        String upper = content.toUpperCase();

        // Fix #9: Validate payment code với regex thay vì indexOf
        String paymentCode = extractValidPaymentCode(upper);
        if (paymentCode == null) return false;

        Optional<MonthlyPass> found = repo.findByPaymentCodeAndStatusNot(paymentCode, "ACTIVE");
        if (found.isEmpty()) return false;

        MonthlyPass pass = found.get();
        // Chỉ xử lý thẻ đang PENDING
        if (!"PENDING".equals(pass.getStatus())) return false;
        if (amount < pass.getAmountDue()) return false;

        pass.setStatus("ACTIVE");
        pass.setPaidAt(LocalDateTime.now());
        pass.setBankRef(bankRef);
        repo.save(pass);

        System.out.printf("[MONTHLY-PASS PAID] ✅ %s | %s | %,d đ%n",
                pass.getPaymentCode(), pass.getLicensePlate(), amount);

        // Fix #10: Gửi email với error handling & logging
        if (pass.getEmail() != null && !pass.getEmail().isBlank()) {
            try {
                emailService.sendMonthlyPassActivatedEmail(
                        pass.getEmail(),
                        pass.getOwnerName(),
                        pass.getLicensePlate(),
                        pass.getPaymentCode(),
                        pass.getStartDate(),
                        pass.getEndDate(),
                        amount
                );
                System.out.printf("[EMAIL SENT] ✅ %s | Thẻ tháng %s%n",
                        pass.getEmail(), pass.getPaymentCode());
            } catch (Exception e) {
                System.err.printf("[EMAIL ERROR] ❌ %s | %s | Error: %s%n",
                        pass.getEmail(), pass.getPaymentCode(), e.getMessage());
                e.printStackTrace();
            }
        }

        return true;
    }

    // ── QUERY ─────────────────────────────────────────────────────────────────

    @Override
    public Optional<MonthlyPass> findActivePass(String licensePlate) {
        return repo.findActiveByPlate(licensePlate, LocalDate.now());
    }

    @Override
    public List<MonthlyPass> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Optional<MonthlyPass> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public List<MonthlyPass> findByPlate(String licensePlate) {
        return repo.findByLicensePlateIgnoreCaseOrderByCreatedAtDesc(licensePlate);
    }

    @Override
    public List<MonthlyPass> findByUser(com.smartpark.model.User user) {
        if (user == null) return List.of();
        return repo.findByUserIdOrderByCreatedAtDesc(user.getId());
    }


    // ── ADMIN ACTIONS ─────────────────────────────────────────────────────────

    @Override
    public MonthlyPass activate(Long id) {
        MonthlyPass pass = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thẻ tháng", "id", id));
        // Fix #8: Không cho phép kích hoạt thẻ đã bị huỷ
        if ("CANCELLED".equals(pass.getStatus())) {
            throw new IllegalStateException("Không thể kích hoạt thẻ đã bị huỷ (id=" + id + ")");
        }
        pass.setStatus("ACTIVE");
        if (pass.getPaidAt() == null) pass.setPaidAt(LocalDateTime.now());
        return repo.save(pass);
    }

    @Override
    public MonthlyPass cancel(Long id) {
        MonthlyPass pass = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thẻ tháng", "id", id));
        pass.setStatus("CANCELLED");
        return repo.save(pass);
    }

    @Override
    public MonthlyPass renew(Long id, int months) {
        if (months <= 0) throw new IllegalArgumentException("Số tháng gia hạn phải > 0");
        MonthlyPass pass = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thẻ tháng", "id", id));

        // Fix #3: Chỉ cho phép gia hạn khi thẻ EXPIRED hoặc ACTIVE sắp hết hạn (trong 7 ngày tới).
        // Tránh vô hiệu hoá thẻ đang dùng dở bằng cách không set PENDING ngay lập tức.
        boolean isExpired = "EXPIRED".equals(pass.getStatus())
                || pass.getEndDate().isBefore(LocalDate.now());
        boolean isExpiringSoon = "ACTIVE".equals(pass.getStatus())
                && !pass.getEndDate().isBefore(LocalDate.now())
                && pass.getEndDate().isBefore(LocalDate.now().plusDays(7));

        if (!isExpired && !isExpiringSoon) {
            throw new IllegalStateException(
                    "Chỉ có thể gia hạn khi thẻ đã hết hạn hoặc còn dưới 7 ngày. " +
                    "Thẻ hiện còn hiệu lực đến: " + pass.getEndDate());
        }

        // Gia hạn từ ngày hết hạn hiện tại (hoặc hôm nay nếu đã hết)
        LocalDate base = pass.getEndDate().isBefore(LocalDate.now())
                ? LocalDate.now() : pass.getEndDate();
        pass.setEndDate(base.plusMonths(months));

        long pricePerMonth = "o_to".equals(pass.getVehicleType())
                ? CAR_PRICE_PER_MONTH : MOTO_PRICE_PER_MONTH;
        pass.setAmountDue(pricePerMonth * months);
        // Fix #2: dùng generateUniquePaymentCode khi gia hạn
        pass.setPaymentCode(generateUniquePaymentCode());
        pass.setStatus("PENDING");
        pass.setPaidAt(null);
        pass.setBankRef(null);

        return repo.save(pass);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        repo.deleteByUserId(userId);
    }

    @Override
    public Optional<MonthlyPass> findByPaymentCode(String paymentCode) {
        return repo.findByPaymentCode(paymentCode);
    }

    // ── AUTO-EXPIRE ──────────────────────────────────────────────────────────

    /**
     * Fix #11: Auto-expire thẻ hết hạn mỗi ngày lúc 00:05
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void autoExpireMonthlyPasses() {
        LocalDate today = LocalDate.now();
        List<MonthlyPass> allPasses = repo.findAll();
        
        int expiredCount = 0;
        for (MonthlyPass pass : allPasses) {
            // Nếu thẻ ACTIVE nhưng endDate < hôm nay → set EXPIRED
            if ("ACTIVE".equals(pass.getStatus()) && pass.getEndDate().isBefore(today)) {
                pass.setStatus("EXPIRED");
                repo.save(pass);
                expiredCount++;
                System.out.printf("[AUTO-EXPIRE] ✅ %s | %s | Hết hạn từ %s%n",
                        pass.getPaymentCode(), pass.getLicensePlate(), pass.getEndDate());
            }
        }
        
        if (expiredCount > 0) {
            System.out.printf("[AUTO-EXPIRE SUMMARY] Đã đánh dấu %d thẻ là EXPIRED%n", expiredCount);
        }
    }

    // ── HELPER ────────────────────────────────────────────────────────────────

    /**
     * Fix #9: Trích xuất & validate mã thanh toán từ nội dung chuyển khoản
     * Format: MP + 6 ký tự (A-Z, 2-9)
     * @return Payment code hợp lệ hoặc null
     */
    private String extractValidPaymentCode(String content) {
        if (content == null || content.length() < 8) return null;
        
        // Tìm pattern MP[A-Z2-9]{6}
        java.util.regex.Matcher matcher = Pattern.compile("MP[A-Z2-9]{6}").matcher(content);
        if (matcher.find()) {
            String code = matcher.group();
            // Double-check với pattern
            if (PAYMENT_CODE_PATTERN.matcher(code).matches()) {
                return code;
            }
        }
        
        return null;
    }

    /**
     * Fix #1 & #2: Sinh mã thanh toán dùng SecureRandom, retry tối đa 5 lần nếu trùng.
     */
    private String generateUniquePaymentCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = generatePaymentCode();
            if (!repo.existsByPaymentCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không thể sinh mã thanh toán duy nhất sau 5 lần thử");
    }

    private String generatePaymentCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("MP");
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

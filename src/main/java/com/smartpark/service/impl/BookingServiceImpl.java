package com.smartpark.service.impl;

import com.smartpark.dto.request.PreBookingRequest;
import com.smartpark.exception.SlotAlreadyOccupiedException;
import com.smartpark.model.Booking;
import com.smartpark.model.MonthlyPass;
import com.smartpark.model.User;
import com.smartpark.repository.BookingRepository;
import com.smartpark.service.BookingService;
import com.smartpark.service.EmailService;
import com.smartpark.service.MonthlyPassService;
import com.smartpark.service.PricingService;
import com.smartpark.service.pricing.PricingFactory;
import com.smartpark.service.pricing.PricingStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation của BookingService.
 * Sử dụng PricingFactory (Factory Pattern) + PricingStrategy (Strategy Pattern)
 * để tính phí thay vì if/else trực tiếp.
 */
@Slf4j
@Primary
@Service
public class BookingServiceImpl implements BookingService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final BookingRepository repo;
    private final PricingFactory pricingFactory;
    private final PricingService pricingService;
    private final MonthlyPassService monthlyPassService;
    private final EmailService emailService;

    public BookingServiceImpl(BookingRepository repo, 
                             PricingFactory pricingFactory,
                             PricingService pricingService,
                             MonthlyPassService monthlyPassService,
                             EmailService emailService) {
        this.repo = repo;
        this.pricingFactory = pricingFactory;
        this.pricingService = pricingService;
        this.monthlyPassService = monthlyPassService;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public Booking createBooking(String customerName, String plate,
                                 String vehicleType,
                                 LocalDateTime checkIn, LocalDateTime checkOut) {
        log.info("Creating walk-in booking for plate: {}, type: {}", plate, vehicleType);
        
        if (checkIn.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new IllegalArgumentException("Ngày check-in không được trong quá khứ");
        }
        
        PricingStrategy pricing = pricingFactory.getStrategy(vehicleType);

        Booking b = new Booking();
        b.setBookingType("walk_in");
        b.setCustomerName(customerName);
        b.setLicensePlate(plate.toUpperCase().trim());
        b.setVehicleType(vehicleType);
        b.setCheckIn(checkIn);
        b.setCheckOut(checkOut);
        b.setAmountDue(pricing.calculate(checkIn, checkOut));
        b.setPaymentCode(generatePaymentCode());
        b.setStatus("CONFIRMED");
        
        Booking saved = repo.save(b);
        log.info("Booking created with ID: {} and PaymentCode: {}", saved.getId(), saved.getPaymentCode());
        return saved;
    }

    @Override
    @Transactional
    public Booking createPreBooking(User user, PreBookingRequest request) {
        // ── THÊM: Kiểm tra ô đỗ đã có booking PENDING/PAID chưa ──
        boolean slotTaken = repo.existsBySlotIdAndStatusIn(
            request.slotId(),
            List.of("PENDING", "PAID")
        );
        if (slotTaken) {
            throw new SlotAlreadyOccupiedException(
                "Ô đỗ " + request.slotId() + " vừa được người khác đặt. Vui lòng chọn ô khác."
            );
        }

        // Kiểm tra user có thẻ tháng hợp lệ không
        Optional<MonthlyPass> activePass = monthlyPassService.findActivePass(
            user != null ? user.getId().toString() : request.licensePlate()
        );
        boolean hasMonthlyPass = activePass.isPresent();

        // Tính giá
        long hours = pricingService.calculateHours(request.startTime(), request.endTime());
        int blocks = pricingService.calculateBlocks12h(hours);
        long price = pricingService.calculatePreBookingPrice(
            request.startTime(),
            request.endTime(),
            request.vehicleType(),
            hasMonthlyPass
        );

        // Tạo booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setBookingType(hasMonthlyPass ? "pre_booking_with_pass" : "pre_booking");
        booking.setCustomerName(user != null ? user.getUsername() : "Guest");
        booking.setEmail(user != null ? user.getEmail() : null);
        booking.setLicensePlate(request.licensePlate());
        booking.setVehicleType(request.vehicleType());
        booking.setBookingDate(request.bookingDate());
        booking.setStartTime(request.startTime());
        booking.setEndTime(request.endTime());
        booking.setDurationHours((double) hours);
        booking.setBlocks12h(blocks);
        booking.setSlotId(request.slotId());
        booking.setAmountDue(price);
        booking.setPaymentCode(generatePaymentCode());
        booking.setStatus("PENDING");

        return repo.save(booking);
    }

    @Override
    public List<Booking> getByUser(Long userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public boolean processPayment(String content, long amount, String bankRef) {
        if (content == null) return false;
        String upper = content.toUpperCase();
        log.info("Processing payment for content: {}, amount: {}", content, amount);

        String paymentCode = null;
        int spIndex = upper.indexOf("SP");
        if (spIndex != -1 && spIndex + 8 <= upper.length()) {
            paymentCode = upper.substring(spIndex, spIndex + 8);
        }
        
        if (paymentCode == null) {
            log.warn("No payment code found in content: {}", content);
            return false;
        }

        Optional<Booking> found = repo.findByPaymentCodeAndStatus(paymentCode, "PENDING");

        if (found.isEmpty()) {
            log.warn("No pending booking found for payment code: {}", paymentCode);
            return false;
        }

        Booking b = found.get();
        if (amount < b.getAmountDue()) {
            log.warn("Insufficient amount for booking {}: expected {}, got {}", paymentCode, b.getAmountDue(), amount);
            return false;
        }

        b.setStatus("PAID");
        b.setPaidAt(LocalDateTime.now());
        b.setBankRef(bankRef);
        repo.save(b);

        if (b.getEmail() != null && !b.getEmail().isBlank()) {
            try {
                emailService.sendBookingPaidEmail(
                    b.getEmail(),
                    b.getCustomerName(),
                    b.getLicensePlate(),
                    b.getPaymentCode(),
                    b.getSlotId(),
                    b.getAmountDue()
                );
            } catch (Exception e) {
                log.error("Failed to send payment confirmation email to {}: {}", b.getEmail(), e.getMessage());
            }
        }

        log.info("[PAID] ✅ {} | {} | {} VND", b.getPaymentCode(), b.getLicensePlate(), amount);
        return true;
    }

    @Override
    public List<Booking> getAll() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public Optional<Booking> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public void deleteAll() {
        repo.deleteAll();
    }

    // ── Private helper ──────────────────────────────────
    private String generatePaymentCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder("SP");
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    // ── Walk-in methods ──────────────────────────────────
    @Override
    public Booking createWalkInBooking(String licensePlate, String vehicleType, String customerName) {
        Booking booking = new Booking();
        booking.setUser(null); // Walk-in không có user
        booking.setBookingType("walk_in");
        booking.setCustomerName(customerName);
        booking.setLicensePlate(licensePlate.toUpperCase().trim());
        booking.setVehicleType(vehicleType);
        booking.setCheckIn(LocalDateTime.now());
        booking.setStatus("PENDING");
        
        return repo.save(booking);
    }
    
    @Override
    public Booking completeWalkIn(Long bookingId, Long actualPrice) {
        Booking booking = repo.findById(bookingId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));
        
        if (!"walk_in".equals(booking.getBookingType())) {
            throw new RuntimeException("Booking này không phải walk-in");
        }
        
        booking.setCheckOut(LocalDateTime.now());
        booking.setAmountDue(actualPrice);
        booking.setStatus("COMPLETED");
        
        return repo.save(booking);
    }

    @Override
    public Optional<Booking> findByPaymentCode(String paymentCode) {
        return repo.findByPaymentCode(paymentCode);
    }

    @Override
    public void cancel(Long bookingId) {
        repo.findById(bookingId).ifPresent(b -> {
            b.setStatus("CANCELLED");
            repo.save(b);
        });
    }
}

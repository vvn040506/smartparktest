package com.smartpark.repository;

import com.smartpark.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByOrderByCreatedAtDesc();
    Optional<Booking> findByPaymentCodeAndStatusNot(String paymentCode, String status);
    Optional<Booking> findByPaymentCodeAndStatus(String paymentCode, String status);
    Optional<Booking> findByPaymentCode(String paymentCode);
    boolean existsBySlotIdAndStatusIn(String slotId, List<String> statuses);
    List<Booking> findByStatusAndCreatedAtBefore(String status, LocalDateTime createdAt);
    List<Booking> findAllByStatus(String status);
    
    /** Tìm booking theo user */
    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Xóa tất cả booking của user */
    void deleteByUserId(Long userId);
}

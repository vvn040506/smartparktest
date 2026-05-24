package com.smartpark.repository;

import com.smartpark.model.MonthlyPass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MonthlyPassRepository extends JpaRepository<MonthlyPass, Long> {

    /** Tìm tất cả thẻ theo biển số (không phân biệt hoa thường) */
    List<MonthlyPass> findByLicensePlateIgnoreCaseOrderByCreatedAtDesc(String licensePlate);

    /** Tìm thẻ đang hiệu lực theo biển số */
    @Query("""
        SELECT p FROM MonthlyPass p
        WHERE UPPER(p.licensePlate) = UPPER(:plate)
          AND p.status = 'ACTIVE'
          AND p.startDate <= :today
          AND p.endDate   >= :today
        """)
    Optional<MonthlyPass> findActiveByPlate(@Param("plate") String plate,
                                            @Param("today") LocalDate today);

    /** Tìm thẻ chờ thanh toán theo mã */
    Optional<MonthlyPass> findByPaymentCodeAndStatusNot(String paymentCode, String status);

    /** Fix #2: Kiểm tra mã thanh toán đã tồn tại chưa (dùng cho retry khi sinh mã) */
    boolean existsByPaymentCode(String paymentCode);

    /** Lấy tất cả thẻ, mới nhất trước */
    List<MonthlyPass> findAllByOrderByCreatedAtDesc();

    /** Đếm thẻ đang hiệu lực */
    long countByStatus(String status);

    /** Tìm thẻ sắp hết hạn (trong N ngày tới) */
    @Query("""
        SELECT p FROM MonthlyPass p
        WHERE p.status = 'ACTIVE'
          AND p.endDate BETWEEN :today AND :deadline
        ORDER BY p.endDate ASC
        """)
    List<MonthlyPass> findExpiringSoon(@Param("today") LocalDate today,
                                       @Param("deadline") LocalDate deadline);

    /** Tìm tất cả thẻ ACTIVE đã quá hạn (dùng cho scheduler) */
    @Query("""
        SELECT p FROM MonthlyPass p
        WHERE p.status = 'ACTIVE'
          AND p.endDate < :today
        """)
    List<MonthlyPass> findActiveButExpired(@Param("today") LocalDate today);
    
    /** Tìm thẻ theo user */
    List<MonthlyPass> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    /** Tìm thẻ ACTIVE của user */
    @Query("""
        SELECT p FROM MonthlyPass p
        WHERE p.user.id = :userId
          AND p.status = 'ACTIVE'
          AND p.startDate <= :today
          AND p.endDate >= :today
        """)
    Optional<MonthlyPass> findActiveByUserId(@Param("userId") Long userId,
                                             @Param("today") LocalDate today);

    /** Xóa tất cả thẻ tháng của user */
    void deleteByUserId(Long userId);
}

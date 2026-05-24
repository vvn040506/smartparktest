package com.smartpark.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thẻ tháng gửi xe — cho phép xe ra vào tự do trong tháng đã đăng ký.
 */
@Entity
@Table(name = "monthly_passes")
@Data
@NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MonthlyPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User sở hữu thẻ (nullable cho trường hợp đăng ký không cần tài khoản) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Họ tên chủ xe */
    private String ownerName;

    /** Email liên hệ (tuỳ chọn) */
    private String email;

    /** Biển số xe (uppercase, không dấu cách) */
    @Column(nullable = false)
    private String licensePlate;

    /** Loại xe: xe_may / o_to */
    @Column(nullable = false)
    private String vehicleType;

    /** Ngày bắt đầu hiệu lực */
    @Column(nullable = false)
    private LocalDate startDate;

    /** Ngày kết thúc hiệu lực (thường = startDate + 30 ngày) */
    @Column(nullable = false)
    private LocalDate endDate;

    /**
     * Trạng thái:
     * PENDING   — chờ thanh toán
     * ACTIVE    — đang hiệu lực
     * EXPIRED   — hết hạn
     * CANCELLED — đã huỷ
     */
    private String status;

    /** Số tiền phải trả (VNĐ) */
    private Long amountDue;

    /** Mã thanh toán (format: MP + 6 ký tự) */
    @Column(unique = true)
    private String paymentCode;

    /** Thời điểm thanh toán thành công */
    private LocalDateTime paidAt;

    /** Mã tham chiếu ngân hàng */
    private String bankRef;

    /** Ghi chú thêm */
    private String note;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }

    /** Kiểm tra thẻ có đang hiệu lực không */
    @Transient
    public boolean isValid() {
        if (!"ACTIVE".equals(this.status)) return false;
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }
}

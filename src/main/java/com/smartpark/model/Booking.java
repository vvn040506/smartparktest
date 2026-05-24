package com.smartpark.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity @Table(name = "bookings")
@Data @NoArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Booking {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User đặt vé (nullable cho walk-in) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Loại booking: walk_in, pre_booking, pre_booking_with_pass */
    private String bookingType;

    private String customerName;
    private String email;
    private String licensePlate;
    private String vehicleType;       // xe_may / o_to

    /** Cho pre-booking: Ngày đặt */
    private java.time.LocalDate bookingDate;
    
    /** Cho pre-booking: Giờ bắt đầu */
    private java.time.LocalTime startTime;
    
    /** Cho pre-booking: Giờ kết thúc */
    private java.time.LocalTime endTime;
    
    /** Cho pre-booking: Số giờ đặt */
    private Double durationHours;
    
    /** Cho pre-booking: Số khung 12h */
    private Integer blocks12h;
    
    /** Cho pre-booking: Vị trí đã đặt */
    private String slotId;

    private LocalDateTime checkIn;
    private LocalDateTime checkOut;

    private String status;            // PENDING / CONFIRMED / PAID
    private String paymentCode;       // mã khách nhập khi chuyển khoản
    private Long   amountDue;

    private LocalDateTime paidAt;
    private String bankRef;
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
    }
}

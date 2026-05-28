package com.smartpark.service;

import com.smartpark.dto.request.PreBookingRequest;
import com.smartpark.model.Booking;
import com.smartpark.model.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service interface quản lý đặt vé / booking.
 * Tách interface từ implementation — thể hiện Abstraction + Dependency Inversion.
 */
public interface BookingService {

    Booking createBooking(String customerName, String plate,
                          String vehicleType,
                          LocalDateTime checkIn, LocalDateTime checkOut);

    /**
     * Tạo booking đặt trước (pre-booking)
     */
    Booking createPreBooking(User user, PreBookingRequest request);

    boolean processPayment(String content, long amount, String bankRef);

    List<Booking> getAll();
    
    /**
     * Lấy danh sách booking của user
     */
    List<Booking> getByUser(Long userId);

    Optional<Booking> findById(Long id);

    void deleteAll();
    
    /**
     * Tạo walk-in booking (đỗ xe trực tiếp không cần đặt trước)
     */
    Booking createWalkInBooking(String licensePlate, String vehicleType, String customerName);
    
    /**
     * Hoàn thành walk-in booking (xe ra, tính tiền)
     */
    Booking completeWalkIn(Long bookingId, Long actualPrice);

    /**
     * Tìm booking theo mã thanh toán
     */
    Optional<Booking> findByPaymentCode(String paymentCode);

    /**
     * Huỷ booking
     */
    void cancel(Long bookingId);
}

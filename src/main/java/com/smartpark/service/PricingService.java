package com.smartpark.service;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Service tính giá đỗ xe
 */
@Service
public class PricingService {

    // Giá đỗ thường (walk-in) - theo khung 12h
    private static final long MOTO_WALK_IN_RATE = 10_000L;
    private static final long CAR_WALK_IN_RATE = 20_000L;

    // Giá đặt trước - theo khung 12h
    private static final long MOTO_PRE_BOOKING_RATE = 15_000L;
    private static final long CAR_PRE_BOOKING_RATE = 30_000L;

    // Giảm giá cho thẻ tháng
    private static final double MONTHLY_PASS_DISCOUNT = 0.50; // 50%

    /**
     * Tính giá đặt trước theo khung 12h
     */
    public long calculatePreBookingPrice(
            LocalTime startTime,
            LocalTime endTime,
            String vehicleType,
            boolean hasMonthlyPass
    ) {
        long hours = calculateHours(startTime, endTime);
        int blocks = calculateBlocks12h(hours);

        long pricePerBlock = "o_to".equals(vehicleType) 
            ? CAR_PRE_BOOKING_RATE 
            : MOTO_PRE_BOOKING_RATE;

        long totalPrice = pricePerBlock * blocks;

        if (hasMonthlyPass) {
            totalPrice = (long) (totalPrice * (1 - MONTHLY_PASS_DISCOUNT));
        }

        return totalPrice;
    }

    /**
     * Tính giá đỗ thường (walk-in) theo khung 12h dựa trên số giờ
     */
    public long calculateWalkInPrice(long hours, String vehicleType) {
        int blocks = calculateBlocks12h(hours);
        long pricePerBlock = "o_to".equals(vehicleType) ? CAR_WALK_IN_RATE : MOTO_WALK_IN_RATE;
        return pricePerBlock * blocks;
    }

    /**
     * Tính giá đỗ thường (walk-in) theo khung 12h dựa trên thời gian thực
     */
    public long calculateWalkInPrice(LocalDateTime checkIn, LocalDateTime checkOut, String vehicleType, boolean hasMonthlyPass) {
        if (checkIn == null || checkOut == null) return 0;
        
        long minutes = Duration.between(checkIn, checkOut).toMinutes();
        double hours = minutes / 60.0;
        int blocks = (int) Math.ceil(hours / 12.0);
        blocks = Math.max(blocks, 1);

        long pricePerBlock = "o_to".equals(vehicleType) 
            ? CAR_WALK_IN_RATE 
            : MOTO_WALK_IN_RATE;

        long totalPrice = pricePerBlock * blocks;

        if (hasMonthlyPass) {
            totalPrice = (long) (totalPrice * (1 - MONTHLY_PASS_DISCOUNT));
        }

        return totalPrice;
    }

    /**
     * Tính số giờ giữa 2 thời điểm (LocalTime)
     */
    public long calculateHours(LocalTime startTime, LocalTime endTime) {
        long hours = Duration.between(startTime, endTime).toHours();
        if (hours <= 0) {
            hours += 24;
        }
        return hours;
    }

    /**
     * Tính số khung 12h (làm tròn lên)
     */
    public int calculateBlocks12h(long hours) {
        int blocks = (int) Math.ceil(hours / 12.0);
        return Math.max(blocks, 1);
    }

    /**
     * Ước tính giá walk-in (1 khung 12h) để hiển thị
     */
    public long estimateWalkInPrice(String vehicleType) {
        return "o_to".equals(vehicleType) ? CAR_WALK_IN_RATE : MOTO_WALK_IN_RATE;
    }

    public double getDiscountRate(boolean hasMonthlyPass) {
        return hasMonthlyPass ? MONTHLY_PASS_DISCOUNT : 0.0;
    }
}
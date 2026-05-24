package com.smartpark.service;

import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalTime;

/**
 * Service tính giá đỗ xe
 */
@Service
public class PricingService {

    // Giá đỗ thường (walk-in) - theo giờ
    private static final long MOTO_HOURLY_RATE = 10_000L;
    private static final long CAR_HOURLY_RATE = 20_000L;

    // Giá đặt trước - theo khung 12h
    private static final long MOTO_PRICE_PER_12H = 20_000L;
    private static final long CAR_PRICE_PER_12H = 40_000L;

    // Giảm giá cho thẻ tháng
    private static final double MONTHLY_PASS_DISCOUNT = 0.20; // 20%

    /**
     * Tính giá đặt trước theo khung 12h
     */
    public long calculatePreBookingPrice(
            LocalTime startTime,
            LocalTime endTime,
            String vehicleType,
            boolean hasMonthlyPass
    ) {
        // Tính số giờ
        long hours = calculateHours(startTime, endTime);

        // Tính số khung 12h (làm tròn lên)
        int blocks = calculateBlocks12h(hours);

        // Giá cơ bản
        long pricePerBlock = "o_to".equals(vehicleType) 
            ? CAR_PRICE_PER_12H 
            : MOTO_PRICE_PER_12H;

        long totalPrice = pricePerBlock * blocks;

        // Giảm giá nếu có thẻ tháng
        if (hasMonthlyPass) {
            totalPrice = (long) (totalPrice * (1 - MONTHLY_PASS_DISCOUNT));
        }

        return totalPrice;
    }

    /**
     * Tính số giờ giữa 2 thời điểm
     */
    public long calculateHours(LocalTime startTime, LocalTime endTime) {
        long hours = Duration.between(startTime, endTime).toHours();
        
        // Nếu endTime < startTime → qua ngày hôm sau
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
        return Math.max(blocks, 1); // Tối thiểu 1 khung
    }

    /**
     * Tính giá đỗ thường (walk-in) theo giờ
     */
    public long calculateWalkInPrice(long hours, String vehicleType) {
        long hourlyRate = "o_to".equals(vehicleType) 
            ? CAR_HOURLY_RATE 
            : MOTO_HOURLY_RATE;
        
        return hourlyRate * hours;
    }
    
    /**
     * Ước tính giá walk-in (1 giờ) để hiển thị cho staff
     */
    public long estimateWalkInPrice(String vehicleType) {
        return calculateWalkInPrice(1, vehicleType);
    }

    /**
     * Kiểm tra có giảm giá không
     */
    public double getDiscountRate(boolean hasMonthlyPass) {
        return hasMonthlyPass ? MONTHLY_PASS_DISCOUNT : 0.0;
    }
}

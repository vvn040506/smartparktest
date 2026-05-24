package com.smartpark.dto.response;

import com.smartpark.model.MonthlyPass;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO trả về thông tin thẻ tháng (không lộ thông tin nhạy cảm).
 */
public record MonthlyPassResponse(
        Long id,
        String ownerName,
        String email,
        String licensePlate,
        String vehicleType,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Long amountDue,
        String paymentCode,
        LocalDateTime paidAt,
        String bankRef,
        String note,
        LocalDateTime createdAt,
        boolean valid
) {
    public static MonthlyPassResponse from(MonthlyPass p) {
        return new MonthlyPassResponse(
                p.getId(),
                p.getOwnerName(),
                p.getEmail(),
                p.getLicensePlate(),
                p.getVehicleType(),
                p.getStartDate(),
                p.getEndDate(),
                p.getStatus(),
                p.getAmountDue(),
                p.getPaymentCode(),
                p.getPaidAt(),
                p.getBankRef(),
                p.getNote(),
                p.getCreatedAt(),
                p.isValid()
        );
    }
}

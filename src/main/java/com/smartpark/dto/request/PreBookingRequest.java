package com.smartpark.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Request đặt trước vị trí đỗ xe
 */
public record PreBookingRequest(
        @NotBlank String licensePlate,
        @NotBlank String vehicleType,  // xe_may / o_to
        @NotNull LocalDate bookingDate,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        String slotId,  // Vị trí muốn đặt (optional)
        String note
) {
    public PreBookingRequest {
        licensePlate = licensePlate != null ? licensePlate.toUpperCase().trim() : null;
        vehicleType = vehicleType != null ? vehicleType.toLowerCase().trim() : null;
    }
}

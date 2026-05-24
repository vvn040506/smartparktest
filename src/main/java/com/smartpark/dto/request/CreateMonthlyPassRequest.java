package com.smartpark.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * DTO tạo thẻ tháng mới.
 */
public record CreateMonthlyPassRequest(

        @NotBlank(message = "Họ tên không được trống")
        String ownerName,

        String email,

        // Fix #5: Validate format biển số xe (VD: 51A-12345 hoặc 51A-1234)
        @NotBlank(message = "Biển số không được trống")
        @Pattern(
            regexp = "^\\d{2}[A-Z]\\d?-\\d{4,5}$",
            message = "Biển số không hợp lệ. Định dạng đúng: 51A-12345 hoặc 51A1-1234"
        )
        String licensePlate,

        // Fix #6: Validate vehicleType chỉ nhận "xe_may" hoặc "o_to"
        @NotBlank(message = "Loại xe không được trống")
        @Pattern(
            regexp = "^(xe_may|o_to)$",
            message = "Loại xe không hợp lệ. Chỉ chấp nhận: xe_may hoặc o_to"
        )
        String vehicleType,

        @NotNull(message = "Ngày bắt đầu không được trống")
        LocalDate startDate,

        /** Số tháng đăng ký (mặc định 1) */
        Integer months,

        String note
) {
    /** Trả về số tháng, mặc định 1 nếu null hoặc <= 0 */
    public int resolvedMonths() {
        return (months != null && months > 0) ? months : 1;
    }
}

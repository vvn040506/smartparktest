package com.smartpark.service;

import com.smartpark.dto.request.CreateMonthlyPassRequest;
import com.smartpark.model.MonthlyPass;

import java.util.List;
import java.util.Optional;

/**
 * Service quản lý thẻ tháng gửi xe.
 */
public interface MonthlyPassService {

    /** Tạo thẻ tháng mới (trạng thái PENDING, chờ thanh toán) */
    MonthlyPass create(CreateMonthlyPassRequest req);

    /** Tạo thẻ tháng mới cho một User nhất định */
    MonthlyPass create(CreateMonthlyPassRequest req, com.smartpark.model.User user);

    /** Tìm danh sách thẻ tháng của một User */
    List<MonthlyPass> findByUser(com.smartpark.model.User user);

    /** Xử lý thanh toán qua webhook (tìm theo paymentCode trong nội dung CK) */
    boolean processPayment(String content, long amount, String bankRef);

    /** Kiểm tra biển số có thẻ tháng hợp lệ không */
    Optional<MonthlyPass> findActivePass(String licensePlate);

    /** Lấy tất cả thẻ */
    List<MonthlyPass> getAll();

    /** Tìm theo id */
    Optional<MonthlyPass> findById(Long id);

    /** Tìm theo biển số */
    List<MonthlyPass> findByPlate(String licensePlate);

    /** Admin kích hoạt thủ công (bỏ qua thanh toán) */
    MonthlyPass activate(Long id);

    /** Admin huỷ thẻ */
    MonthlyPass cancel(Long id);

    /** Gia hạn thêm N tháng */
    MonthlyPass renew(Long id, int months);

    /** Xóa tất cả thẻ tháng của user */
    void deleteByUserId(Long userId);

    /** Tìm thẻ tháng theo mã thanh toán */
    Optional<MonthlyPass> findByPaymentCode(String paymentCode);
}

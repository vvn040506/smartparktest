# 📝 Implementation Log - SmartPark Pre-booking

## ✅ Phase 1: Liên kết User & Pre-booking Foundation (COMPLETED)

### **1. Model Updates**

#### ✅ MonthlyPass.java
- Thêm `@ManyToOne User user` - Liên kết với User
- Nullable để hỗ trợ đăng ký không cần tài khoản

#### ✅ Booking.java
- Thêm `@ManyToOne User user` - Liên kết với User
- Thêm `bookingType` - Phân loại: walk_in, pre_booking, pre_booking_with_pass
- Thêm fields cho pre-booking:
  - `bookingDate` - Ngày đặt
  - `startTime` - Giờ bắt đầu
  - `endTime` - Giờ kết thúc
  - `durationHours` - Số giờ đặt
  - `blocks12h` - Số khung 12h
  - `slotId` - Vị trí đã đặt

### **2. DTOs**

#### ✅ PreBookingRequest.java (NEW)
```java
public record PreBookingRequest(
    String licensePlate,
    String vehicleType,
    LocalDate bookingDate,
    LocalTime startTime,
    LocalTime endTime,
    String slotId,
    String note
)
```

### **3. Services**

#### ✅ PricingService.java (NEW)
- `calculatePreBookingPrice()` - Tính giá theo khung 12h
- `calculateHours()` - Tính số giờ
- `calculateBlocks12h()` - Tính số khung (làm tròn lên)
- `calculateWalkInPrice()` - Tính giá đỗ thường
- Hỗ trợ giảm giá 20% cho thẻ tháng

**Giá cơ bản:**
- Xe máy: 20.000đ/khung 12h
- Ô tô: 40.000đ/khung 12h
- Giảm 20% nếu có thẻ tháng

#### ✅ BookingService.java
- Thêm method `createPreBooking(User, PreBookingRequest)`
- Thêm method `getByUser(Long userId)`

#### ✅ BookingServiceImpl.java
- Implement `createPreBooking()`:
  - Kiểm tra user có thẻ tháng
  - Tính giá theo khung 12h
  - Tự động giảm 20% nếu có thẻ
  - Set bookingType phù hợp
- Implement `getByUser()`
- Cập nhật `createBooking()` set bookingType = "walk_in"
- Đổi Random → SecureRandom (bảo mật)

### **4. Repositories**

#### ✅ BookingRepository.java
- Thêm `findByUserIdOrderByCreatedAtDesc(Long userId)`

#### ✅ MonthlyPassRepository.java
- Thêm `findByUserIdOrderByCreatedAtDesc(Long userId)`
- Thêm `findActiveByUserId(Long userId, LocalDate today)`

---

## 🔄 Phase 2: QR Code Generation (NEXT)

### **TODO:**
- [ ] Thêm dependency ZXing
- [ ] Tạo QRCodeService
- [ ] Generate QR cho Booking
- [ ] Generate QR cho MonthlyPass
- [ ] Cập nhật templates hiển thị QR

---

## 🔄 Phase 3: Pre-booking UI (NEXT)

### **TODO:**
- [ ] Tạo trang `/pre-booking` (form đặt trước)
- [ ] Tính giá real-time bằng JavaScript
- [ ] Hiển thị giảm giá nếu có thẻ tháng
- [ ] Tạo trang chi tiết booking có QR
- [ ] Webhook xử lý thanh toán pre-booking

---

## 🔄 Phase 4: My Account Page (NEXT)

### **TODO:**
- [ ] Tạo trang `/my-account`
- [ ] Hiển thị thẻ tháng hiện tại
- [ ] Hiển thị lịch sử booking
- [ ] Link đến đặt trước và đăng ký thẻ

---

## 🔄 Phase 5: Staff QR Scanner (NEXT)

### **TODO:**
- [ ] Tạo trang `/staff/scan-qr`
- [ ] Tích hợp camera scanner
- [ ] API verify QR
- [ ] Hiển thị thông tin sau khi quét

---

## 📊 Database Migration Needed

Khi deploy, cần chạy migration:

```sql
-- Thêm cột user_id vào monthly_passes
ALTER TABLE monthly_passes 
ADD COLUMN user_id BIGINT REFERENCES users(id);

CREATE INDEX idx_monthly_passes_user_id ON monthly_passes(user_id);

-- Thêm các cột mới vào bookings
ALTER TABLE bookings 
ADD COLUMN user_id BIGINT REFERENCES users(id),
ADD COLUMN booking_type VARCHAR(50),
ADD COLUMN booking_date DATE,
ADD COLUMN start_time TIME,
ADD COLUMN end_time TIME,
ADD COLUMN duration_hours DECIMAL(5,2),
ADD COLUMN blocks_12h INT,
ADD COLUMN slot_id VARCHAR(50);

CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_date ON bookings(booking_date);
CREATE INDEX idx_bookings_type ON bookings(booking_type);
```

**Lưu ý:** Spring Boot với `ddl-auto=update` sẽ tự động tạo các cột mới khi khởi động.

---

## 🧪 Testing

### **Unit Tests cần viết:**
- [ ] PricingService.calculatePreBookingPrice()
- [ ] PricingService.calculateBlocks12h()
- [ ] BookingService.createPreBooking()

### **Integration Tests:**
- [ ] Tạo pre-booking với thẻ tháng → Giảm 20%
- [ ] Tạo pre-booking không thẻ → Giá gốc
- [ ] Tính giá 11h → 1 khung
- [ ] Tính giá 13h → 2 khung

---

## 📝 Notes

### **Công thức tính giá:**
```
Số khung = CEILING(Số giờ / 12)
Giá = Số khung × Giá cơ bản × (Có thẻ ? 0.8 : 1.0)
```

### **Ví dụ:**
- 11h xe máy = 1 khung × 20k = 20k (có thẻ: 16k)
- 13h xe máy = 2 khung × 20k = 40k (có thẻ: 32k)
- 11h ô tô = 1 khung × 40k = 40k (có thẻ: 32k)

### **Booking Types:**
- `walk_in` - Đỗ thường, không đặt trước
- `pre_booking` - Đặt trước, không có thẻ tháng
- `pre_booking_with_pass` - Đặt trước, có thẻ tháng (giảm 20%)

---

## ⚠️ Breaking Changes

Không có breaking changes. Tất cả thay đổi đều backward compatible:
- User nullable → Hỗ trợ cả có và không có tài khoản
- Booking fields mới nullable → Không ảnh hưởng walk-in cũ
- bookingType mặc định null → Có thể set sau

---

## 🎯 Next Steps

1. **Test Phase 1:**
   ```bash
   mvn clean test
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```

2. **Verify database:**
   - Kiểm tra bảng có cột mới
   - Test tạo booking với user

3. **Start Phase 2:**
   - Implement QR Code generation

---

**Status:** Phase 1 COMPLETED ✅  
**Next:** Phase 2 - QR Code Generation  
**ETA:** 2-3 hours

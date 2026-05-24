# 🎯 Mô hình kinh doanh SmartPark - Đề xuất

## 📋 Tổng quan

### **2 loại khách hàng:**

#### 1. **Khách vãng lai** (Walk-in)
- Đỗ xe trực tiếp tại bãi
- Báo nhân viên → Nhân viên nhập hệ thống
- Thanh toán khi ra (theo giờ)
- **KHÔNG CẦN** đăng nhập/đăng ký

#### 2. **Khách thành viên** (Member)
- Đăng ký tài khoản
- Đặt trước vị trí online
- Mua thẻ tháng
- Có ưu đãi đặt trước nếu có thẻ tháng
- Có QR code minh chứng

---

## 🎫 Các loại vé/thẻ

### **1. Vé đỗ xe thường (Walk-in)**
```
Khách vào bãi → Nhân viên check-in → Đỗ xe → Ra → Thanh toán
```
- Không cần đăng ký
- Không có QR code
- Thanh toán tại chỗ

### **2. Vé đặt trước (Pre-booking)**
```
Đăng nhập → Chọn vị trí → Đặt trước → Thanh toán online → Nhận QR
```
- **Yêu cầu:** Đăng nhập
- **Có:** QR code để check-in
- **Giá:** Bình thường

### **3. Thẻ tháng (Monthly Pass)**
```
Đăng nhập → Đăng ký thẻ tháng → Thanh toán → Nhận QR thẻ tháng
```
- **Yêu cầu:** Đăng nhập
- **Có:** QR code thẻ tháng
- **Ưu đãi:** Ra vào tự do + Giảm giá đặt trước

### **4. Vé đặt trước (có thẻ tháng)**
```
Đăng nhập → Chọn vị trí → Đặt trước → Giảm giá 20% → Nhận QR
```
- **Yêu cầu:** Có thẻ tháng hợp lệ
- **Có:** QR code vé đặt trước
- **Giá:** Giảm 20% (hoặc tùy chỉnh)

---

## 💰 Bảng giá đề xuất

### **Giá đỗ thường (Walk-in):**
| Loại xe | Giá/giờ |
|---------|---------|
| Xe máy | 5.000đ |
| Ô tô | 15.000đ |

### **Giá đặt trước (Pre-booking) - Tính theo khung 12h:**

**Công thức:** `Giá = (Số khung 12h) × Giá cơ bản`

| Thời gian đặt | Số khung 12h | Xe máy | Xe máy (có thẻ) | Ô tô | Ô tô (có thẻ) |
|---------------|--------------|--------|-----------------|------|---------------|
| **≤ 12h** | 1 | 15.000đ | 12.000đ (-20%) | 40.000đ | 32.000đ (-20%) |
| **13-24h** | 2 | 30.000đ | 24.000đ (-20%) | 80.000đ | 64.000đ (-20%) |
| **25-36h** | 3 | 45.000đ | 36.000đ (-20%) | 120.000đ | 96.000đ (-20%) |

**Ví dụ:**
- Đặt từ 8h sáng → 11h sáng (3 tiếng) = **1 khung** = 15.000đ (xe máy)
- Đặt từ 8h sáng → 13h chiều (5 tiếng) = **1 khung** = 15.000đ (xe máy)
- Đặt từ 8h sáng → 20h tối (12 tiếng) = **1 khung** = 15.000đ (xe máy)
- Đặt từ 8h sáng → 21h tối (13 tiếng) = **2 khung** = 30.000đ (xe máy)
- Đặt từ 8h sáng hôm nay → 8h sáng hôm sau (24 tiếng) = **2 khung** = 30.000đ (xe máy)

### **Thẻ tháng:**
| Loại xe | Giá/tháng |
|---------|-----------|
| Xe máy | 200.000đ |
| Ô tô | 500.000đ |

**Ưu đãi:** Ra vào tự do + Giảm 20% khi đặt trước

---

## 🔄 Flow hoạt động

### **Flow 1: Khách vãng lai**
```
1. Khách đến bãi xe
2. Nhân viên: "Biển số xe?"
3. Nhân viên nhập vào hệ thống (check-in)
4. Khách đỗ xe
5. Khách ra → Nhân viên tính tiền (check-out)
6. Thanh toán tại chỗ
```

### **Flow 2: Đặt trước (không có thẻ tháng)**
```
1. Khách đăng nhập website/app
2. Chọn "Đặt trước vị trí"
3. Chọn ngày, giờ bắt đầu, giờ kết thúc, loại xe
4. Hệ thống tính: 
   - Số giờ = 13 giờ
   - Số khung 12h = 2 khung (làm tròn lên)
   - Giá = 15.000đ × 2 = 30.000đ (xe máy)
5. Thanh toán online
6. Nhận QR code vé đặt trước
7. Đến bãi → Quét QR → Vào vị trí đã đặt
```

### **Flow 3: Mua thẻ tháng**
```
1. Khách đăng nhập
2. Chọn "Đăng ký thẻ tháng"
3. Điền thông tin (biển số, loại xe)
4. Thanh toán online (200k xe máy / 500k ô tô)
5. Nhận QR code thẻ tháng
6. Ra vào tự do trong tháng (quét QR)
```

### **Flow 4: Đặt trước (có thẻ tháng)**
```
1. Khách đăng nhập (có thẻ tháng hợp lệ)
2. Chọn "Đặt trước vị trí"
3. Chọn thời gian: 8h sáng → 20h tối (12 giờ)
4. Hệ thống tính:
   - Số khung 12h = 1 khung
   - Giá gốc = 15.000đ
   - Giảm 20% = -3.000đ
   - Tổng = 12.000đ (xe máy)
5. Thanh toán
6. Nhận QR code vé đặt trước
7. Đến bãi → Quét QR → Vào vị trí đã đặt
```

---

## 🎫 QR Code cho từng loại

### **1. QR Vé đặt trước**
```json
{
  "type": "booking",
  "id": 123,
  "plate": "30A12345",
  "code": "SP123456",
  "slot": "A-05",
  "date": "2026-05-23",
  "time": "14:00"
}
```

### **2. QR Thẻ tháng**
```json
{
  "type": "monthly_pass",
  "id": 456,
  "plate": "30A12345",
  "code": "MP123456",
  "validUntil": "2026-06-22"
}
```

### **3. Nhân viên quét QR**
- Quét QR → Hiển thị thông tin
- Kiểm tra hợp lệ → Cho vào
- Tự động check-in vào hệ thống

---

## 🏗️ Thay đổi cần implement

### **1. Thêm quan hệ User ↔ Booking/MonthlyPass**

#### Model MonthlyPass
```java
@Entity
public class MonthlyPass {
    // ... fields cũ
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;  // Thêm quan hệ với User
}
```

#### Model Booking
```java
@Entity
public class Booking {
    // ... fields cũ
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;  // Thêm quan hệ với User (nullable cho walk-in)
    
    private String bookingType; // "walk_in", "pre_booking", "pre_booking_with_pass"
}
```

---

### **2. Tạo trang "Tài khoản của tôi"**

#### URL: `/my-account`

**Hiển thị:**
- Thông tin cá nhân
- Thẻ tháng hiện tại (nếu có)
- Lịch sử đặt vé
- Nút "Đặt trước vị trí"
- Nút "Đăng ký thẻ tháng"

---

### **3. Tính năng đặt trước vị trí**

#### Trang: `/booking/pre-book`

**Form đặt trước:**
```html
<form>
  <select name="slotId">
    <option>A-01 (Trống)</option>
    <option>A-02 (Trống)</option>
  </select>
  
  <input type="date" name="bookingDate" required>
  
  <div class="time-range">
    <label>Giờ bắt đầu:</label>
    <input type="time" name="startTime" required>
    
    <label>Giờ kết thúc:</label>
    <input type="time" name="endTime" required>
  </div>
  
  <select name="vehicleType">
    <option value="xe_may">Xe máy</option>
    <option value="o_to">Ô tô</option>
  </select>
  
  <div class="price-calculation">
    <div>Thời gian: <strong id="duration">0 giờ</strong></div>
    <div>Số khung 12h: <strong id="blocks">0</strong></div>
    <div>Giá cơ bản: <strong id="basePrice">0đ</strong></div>
    <div class="discount" th:if="${hasMonthlyPass}">
      <span>Giảm giá (Thẻ tháng): -20%</span>
      <strong id="discount">0đ</strong>
    </div>
    <hr>
    <div class="total">
      Tổng thanh toán: <strong id="finalPrice">0đ</strong>
    </div>
  </div>
  
  <button>Đặt và thanh toán</button>
</form>

<script>
// Tính giá real-time khi thay đổi thời gian
function calculatePrice() {
  const startTime = document.querySelector('[name="startTime"]').value;
  const endTime = document.querySelector('[name="endTime"]').value;
  const vehicleType = document.querySelector('[name="vehicleType"]').value;
  const hasPass = [[${hasMonthlyPass}]];
  
  if (!startTime || !endTime) return;
  
  // Tính số giờ
  const start = new Date('2000-01-01 ' + startTime);
  const end = new Date('2000-01-01 ' + endTime);
  let hours = (end - start) / (1000 * 60 * 60);
  
  if (hours <= 0) {
    // Qua ngày hôm sau
    hours += 24;
  }
  
  // Tính số khung 12h (làm tròn lên)
  const blocks = Math.ceil(hours / 12);
  
  // Giá cơ bản
  const pricePerBlock = vehicleType === 'o_to' ? 40000 : 15000;
  const basePrice = pricePerBlock * blocks;
  
  // Giảm giá nếu có thẻ tháng
  const discount = hasPass ? basePrice * 0.2 : 0;
  const finalPrice = basePrice - discount;
  
  // Hiển thị
  document.getElementById('duration').textContent = hours.toFixed(1) + ' giờ';
  document.getElementById('blocks').textContent = blocks;
  document.getElementById('basePrice').textContent = basePrice.toLocaleString() + 'đ';
  if (hasPass) {
    document.getElementById('discount').textContent = '-' + discount.toLocaleString() + 'đ';
  }
  document.getElementById('finalPrice').textContent = finalPrice.toLocaleString() + 'đ';
}

document.querySelector('[name="startTime"]').addEventListener('change', calculatePrice);
document.querySelector('[name="endTime"]').addEventListener('change', calculatePrice);
document.querySelector('[name="vehicleType"]').addEventListener('change', calculatePrice);
</script>
```

#### Service
```java
@Service
public class BookingService {
    
    // Giá cơ bản cho 1 khung 12h
    private static final long MOTO_BASE_PRICE_12H = 15_000L;
    private static final long CAR_BASE_PRICE_12H = 40_000L;
    
    public Booking createPreBooking(User user, PreBookingRequest req) {
        // Kiểm tra user có thẻ tháng hợp lệ không
        boolean hasPass = monthlyPassService.findActivePass(user.getId()).isPresent();
        
        // Tính số giờ đặt
        LocalDateTime startTime = LocalDateTime.of(req.bookingDate(), req.startTime());
        LocalDateTime endTime = LocalDateTime.of(req.bookingDate(), req.endTime());
        long hours = Duration.between(startTime, endTime).toHours();
        
        // Tính số khung 12h (làm tròn lên)
        int blocks = (int) Math.ceil(hours / 12.0);
        if (blocks < 1) blocks = 1;
        
        // Tính giá
        long basePrice = req.vehicleType().equals("o_to") 
            ? CAR_BASE_PRICE_12H 
            : MOTO_BASE_PRICE_12H;
        
        long totalPrice = basePrice * blocks;
        
        // Giảm giá 20% nếu có thẻ tháng
        long finalPrice = hasPass ? (long)(totalPrice * 0.8) : totalPrice;
        
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setBookingType(hasPass ? "pre_booking_with_pass" : "pre_booking");
        booking.setSlotId(req.slotId());
        booking.setBookingDate(req.bookingDate());
        booking.setStartTime(req.startTime());
        booking.setEndTime(req.endTime());
        booking.setDurationHours(hours);
        booking.setBlocks12h(blocks);
        booking.setAmountDue(finalPrice);
        booking.setPaymentCode(generateCode("SP"));
        booking.setStatus("PENDING");
        
        return bookingRepo.save(booking);
    }
}
```

---

### **4. Tạo QR Code cho vé và thẻ**

#### QRCodeService
```java
@Service
public class QRCodeService {
    
    public String generateBookingQR(Booking booking) {
        String data = String.format(
            "SMARTPARK_BOOKING|%d|%s|%s|%s|%s",
            booking.getId(),
            booking.getPaymentCode(),
            booking.getLicensePlate(),
            booking.getBookingDate(),
            booking.getSlotId()
        );
        return generateQRImage(data);
    }
    
    public String generatePassQR(MonthlyPass pass) {
        String data = String.format(
            "SMARTPARK_PASS|%d|%s|%s|%s",
            pass.getId(),
            pass.getPaymentCode(),
            pass.getLicensePlate(),
            pass.getEndDate()
        );
        return generateQRImage(data);
    }
    
    private String generateQRImage(String data) {
        // Dùng ZXing tạo QR code
        // Return base64 image
    }
}
```

---

### **5. Trang quét QR cho nhân viên**

#### URL: `/staff/scan-qr`

```html
<div class="scanner-page">
    <h3>Quét QR Code</h3>
    
    <!-- Camera scanner -->
    <div id="qr-reader"></div>
    
    <!-- Hoặc nhập thủ công -->
    <input type="text" placeholder="Hoặc nhập mã: SP123456">
    
    <div id="result"></div>
</div>

<script src="https://unpkg.com/html5-qrcode"></script>
<script>
const html5QrCode = new Html5Qrcode("qr-reader");

html5QrCode.start(
    { facingMode: "environment" },
    { fps: 10, qrbox: 250 },
    async (decodedText) => {
        // Gửi lên server verify
        const res = await fetch('/api/verify-qr', {
            method: 'POST',
            body: JSON.stringify({ qrData: decodedText })
        });
        
        const data = await res.json();
        
        if (data.success) {
            showSuccess(data.data);
        } else {
            showError(data.message);
        }
    }
);
</script>
```

#### API Verify
```java
@PostMapping("/api/verify-qr")
public ApiResponse<?> verifyQR(@RequestBody Map<String, String> payload) {
    String qrData = payload.get("qrData");
    String[] parts = qrData.split("\\|");
    
    if (parts[0].equals("SMARTPARK_BOOKING")) {
        // Verify booking
        return verifyBooking(parts);
    } else if (parts[0].equals("SMARTPARK_PASS")) {
        // Verify monthly pass
        return verifyPass(parts);
    }
    
    return ApiResponse.error("QR không hợp lệ");
}
```

---

### **6. Dashboard nhân viên**

#### Thêm chức năng:
- ✅ Check-in walk-in (nhập biển số)
- ✅ Quét QR vé đặt trước
- ✅ Quét QR thẻ tháng
- ✅ Check-out và tính tiền
- ✅ Xem danh sách xe đang đỗ

---

## 📊 Database Schema Changes

### **Bảng users**
```sql
-- Đã có sẵn
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    created_at TIMESTAMP
);
```

### **Bảng monthly_passes**
```sql
ALTER TABLE monthly_passes 
ADD COLUMN user_id BIGINT REFERENCES users(id);

-- Index
CREATE INDEX idx_monthly_passes_user_id ON monthly_passes(user_id);
```

### **Bảng bookings**
```sql
ALTER TABLE bookings 
ADD COLUMN user_id BIGINT REFERENCES users(id),
ADD COLUMN booking_type VARCHAR(50), -- 'walk_in', 'pre_booking', 'pre_booking_with_pass'
ADD COLUMN booking_date DATE,
ADD COLUMN booking_time TIME,
ADD COLUMN slot_id VARCHAR(50);

-- Index
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_date ON bookings(booking_date);
```

---

## 🎯 Roadmap Implementation

### **Phase 1: Liên kết User (1-2 ngày)**
- [ ] Sửa Model: Thêm `@ManyToOne User` vào MonthlyPass, Booking
- [ ] Migration database
- [ ] Sửa Service: Lưu user_id khi tạo thẻ/vé
- [ ] Tạo trang "Tài khoản của tôi"

### **Phase 2: QR Code (2-3 ngày)**
- [ ] Thêm dependency ZXing
- [ ] Tạo QRCodeService
- [ ] Tạo QR cho vé đặt trước
- [ ] Tạo QR cho thẻ tháng
- [ ] Trang chi tiết hiển thị QR
- [ ] Nút tải QR code

### **Phase 3: Đặt trước vị trí (3-4 ngày)**
- [ ] Tạo trang đặt trước `/booking/pre-book`
- [ ] Logic kiểm tra thẻ tháng → Giảm giá
- [ ] Tạo payment code + QR thanh toán
- [ ] Webhook xử lý thanh toán
- [ ] Gửi email có QR vé

### **Phase 4: Quét QR (2-3 ngày)**
- [ ] Trang quét QR cho nhân viên
- [ ] API verify QR (booking + pass)
- [ ] Tích hợp camera scanner
- [ ] Tự động check-in khi quét

### **Phase 5: Ưu đãi thẻ tháng (1 ngày)**
- [ ] Logic giảm giá khi có thẻ tháng
- [ ] Hiển thị badge "Thành viên VIP"
- [ ] Thống kê tiết kiệm được

### **Phase 6: Polish (1-2 ngày)**
- [ ] Email templates đẹp
- [ ] Responsive mobile
- [ ] Testing đầy đủ
- [ ] Documentation

**Tổng thời gian: 10-15 ngày**

---

## 💡 Tóm tắt

### **Mô hình kinh doanh:**
1. **Walk-in:** Đỗ thường, không cần đăng ký
2. **Pre-booking:** Đặt trước, có QR, cần đăng nhập
3. **Monthly Pass:** Thẻ tháng, có QR, ra vào tự do
4. **Pre-booking + Pass:** Đặt trước với giảm giá 25%

### **Lợi ích:**
- ✅ Tăng trải nghiệm khách hàng
- ✅ Khuyến khích mua thẻ tháng
- ✅ Tăng doanh thu từ đặt trước
- ✅ Quản lý chặt chẽ hơn với QR

### **Công nghệ:**
- ✅ QR Code (ZXing)
- ✅ User authentication
- ✅ Discount logic
- ✅ Camera scanner

Bạn muốn tôi bắt đầu implement từ Phase nào?

# 🔄 Phân tích: Tính năng Tự động gia hạn Thẻ tháng

## ❌ Hiện trạng: KHÔNG có tự động gia hạn

### **Có sẵn:**

#### 1. **Gia hạn thủ công** (Admin hoặc API)
```java
// MonthlyPassService.renew(id, months)
// Chỉ cho phép gia hạn khi:
// - Thẻ đã EXPIRED
// - Hoặc thẻ ACTIVE còn dưới 7 ngày
```

**Cách hoạt động:**
- Admin vào dashboard → Chọn thẻ → Nhấn "Gia hạn"
- Hoặc gọi API: `POST /api/monthly-passes/{id}/renew?months=2`
- Tạo mã thanh toán mới → Khách phải thanh toán lại

#### 2. **Scheduler tự động đánh dấu EXPIRED**
```java
// MonthlyPassScheduler.markExpiredPasses()
// Chạy mỗi ngày lúc 00:05
// Tìm thẻ ACTIVE có endDate < hôm nay → set EXPIRED
```

**Nghĩa là:**
- ✅ Hệ thống tự động đánh dấu thẻ hết hạn
- ❌ **KHÔNG** tự động gia hạn
- ❌ **KHÔNG** tự động thu tiền

---

## 🎯 Tự động gia hạn là gì?

### **Mô hình subscription (đăng ký định kỳ):**

1. **Khách đăng ký lần đầu** → Thanh toán tháng 1
2. **Trước khi hết hạn** → Hệ thống tự động:
   - Tạo hóa đơn tháng tiếp theo
   - Thu tiền từ thẻ/tài khoản đã lưu
   - Gia hạn thẻ tự động
3. **Khách không cần làm gì** → Thẻ tự động gia hạn mỗi tháng

### **Ví dụ thực tế:**
- Netflix: Tự động trừ tiền mỗi tháng
- Spotify: Tự động gia hạn Premium
- Grab Unlimited: Tự động gia hạn gói cước

---

## 💡 Các cách implement Tự động gia hạn

### **Cách 1: Lưu thông tin thẻ (Credit/Debit Card)**

#### Yêu cầu:
- Tích hợp Payment Gateway (Stripe, PayPal, VNPay)
- Lưu token thẻ (không lưu số thẻ thật)
- PCI-DSS compliance

#### Flow:
```
1. Khách đăng ký → Nhập thẻ → Lưu token
2. Trước hết hạn 3 ngày → Tự động charge thẻ
3. Thành công → Gia hạn thẻ
4. Thất bại → Gửi email nhắc thanh toán
```

#### Code mẫu:
```java
@Scheduled(cron = "0 0 9 * * *") // Chạy 9h sáng mỗi ngày
@Transactional
public void autoRenewPasses() {
    LocalDate threeDaysLater = LocalDate.now().plusDays(3);
    
    // Tìm thẻ sắp hết hạn trong 3 ngày
    List<MonthlyPass> expiring = repo.findActiveExpiringBetween(
        LocalDate.now(), threeDaysLater
    );
    
    for (MonthlyPass pass : expiring) {
        if (pass.getAutoRenew() && pass.getPaymentToken() != null) {
            try {
                // Charge thẻ
                long amount = calculateAmount(pass.getVehicleType(), 1);
                boolean success = paymentGateway.charge(
                    pass.getPaymentToken(), 
                    amount
                );
                
                if (success) {
                    // Gia hạn thẻ
                    pass.setEndDate(pass.getEndDate().plusMonths(1));
                    pass.setStatus("ACTIVE");
                    repo.save(pass);
                    
                    // Gửi email xác nhận
                    emailService.sendAutoRenewalSuccess(pass);
                } else {
                    // Gửi email thất bại
                    emailService.sendAutoRenewalFailed(pass);
                }
            } catch (Exception e) {
                log.error("Auto-renewal failed for pass {}", pass.getId(), e);
            }
        }
    }
}
```

#### Ưu điểm:
- ✅ Hoàn toàn tự động
- ✅ Khách không cần làm gì
- ✅ Trải nghiệm tốt nhất

#### Nhược điểm:
- ❌ Phức tạp, cần Payment Gateway
- ❌ Chi phí phí giao dịch (2-3%)
- ❌ Yêu cầu bảo mật cao
- ❌ Cần giấy phép kinh doanh thanh toán

---

### **Cách 2: Chuyển khoản định kỳ (Standing Order)**

#### Flow:
```
1. Khách đăng ký → Thiết lập lệnh CK định kỳ tại ngân hàng
2. Mỗi tháng ngân hàng tự động chuyển tiền
3. Webhook nhận thông báo → Gia hạn thẻ
```

#### Ưu điểm:
- ✅ Không cần lưu thông tin thẻ
- ✅ Khách tự quản lý tại ngân hàng
- ✅ Đơn giản hơn

#### Nhược điểm:
- ❌ Khách phải tự setup tại ngân hàng
- ❌ Không phải ngân hàng nào cũng hỗ trợ
- ❌ Khó kiểm soát

---

### **Cách 3: Nhắc nhở + Link thanh toán nhanh**

#### Flow:
```
1. Trước hết hạn 7 ngày → Gửi email/SMS nhắc nhở
2. Email chứa link thanh toán 1-click
3. Khách click → Thanh toán → Tự động gia hạn
```

#### Code mẫu:
```java
@Scheduled(cron = "0 0 9 * * *")
public void sendRenewalReminders() {
    LocalDate sevenDaysLater = LocalDate.now().plusDays(7);
    
    List<MonthlyPass> expiring = repo.findActiveExpiringOn(sevenDaysLater);
    
    for (MonthlyPass pass : expiring) {
        // Tạo link gia hạn nhanh
        String renewToken = generateRenewToken(pass);
        String renewUrl = String.format(
            "https://your-domain.com/renew/%s", 
            renewToken
        );
        
        // Gửi email
        emailService.sendRenewalReminder(
            pass.getEmail(),
            pass.getOwnerName(),
            pass.getLicensePlate(),
            pass.getEndDate(),
            renewUrl
        );
    }
}

@GetMapping("/renew/{token}")
public String quickRenew(@PathVariable String token, Model model) {
    MonthlyPass pass = findByRenewToken(token);
    
    // Tự động tạo đơn gia hạn
    MonthlyPass renewed = monthlyPassService.renew(pass.getId(), 1);
    
    // Chuyển đến trang thanh toán
    return "redirect:/the-thang/" + renewed.getId();
}
```

#### Email mẫu:
```html
<h2>🔔 Thẻ tháng sắp hết hạn</h2>
<p>Xin chào <strong>Nguyễn Văn A</strong>,</p>
<p>Thẻ tháng của bạn sẽ hết hạn vào <strong>22/06/2026</strong></p>
<p>Biển số: <strong>30A12345</strong></p>
<p>
  <a href="https://your-domain.com/renew/abc123xyz" 
     style="background:#1b4f8a;color:white;padding:12px 24px;text-decoration:none;border-radius:8px">
    🔄 Gia hạn ngay (500.000đ)
  </a>
</p>
```

#### Ưu điểm:
- ✅ Đơn giản, dễ implement
- ✅ Không cần lưu thông tin thẻ
- ✅ Khách chủ động quyết định

#### Nhược điểm:
- ❌ Không hoàn toàn tự động
- ❌ Khách vẫn phải click + thanh toán

---

### **Cách 4: Ví điện tử/Tài khoản nội bộ**

#### Flow:
```
1. Khách nạp tiền vào ví SmartPark
2. Hệ thống tự động trừ ví mỗi tháng
3. Hết tiền → Gửi thông báo nạp thêm
```

#### Ưu điểm:
- ✅ Hoàn toàn tự động
- ✅ Không phí giao dịch
- ✅ Kiểm soát tốt

#### Nhược điểm:
- ❌ Phức tạp, cần quản lý ví
- ❌ Khách phải nạp tiền trước
- ❌ Cần giấy phép ví điện tử

---

## 🎯 So sánh giải pháp

| Giải pháp | Độ tự động | Độ khó | Chi phí | Khuyến nghị |
|-----------|------------|--------|---------|-------------|
| **Lưu thẻ + Auto charge** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | Cao | Doanh nghiệp lớn |
| **Chuyển khoản định kỳ** | ⭐⭐⭐⭐ | ⭐⭐ | Thấp | Khách quen |
| **Nhắc nhở + Link nhanh** | ⭐⭐⭐ | ⭐⭐ | Thấp | **Khuyến nghị** |
| **Ví nội bộ** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | Trung bình | Startup |

---

## 💡 Khuyến nghị: Cách 3 (Nhắc nhở + Link nhanh)

### **Lý do:**
- ✅ Dễ implement nhất
- ✅ Không cần Payment Gateway phức tạp
- ✅ Không cần lưu thông tin thẻ
- ✅ Khách vẫn có trải nghiệm tốt
- ✅ Chi phí thấp

### **Implement:**

#### 1. Thêm Scheduler gửi nhắc nhở
```java
@Scheduled(cron = "0 0 9 * * *") // 9h sáng mỗi ngày
public void sendRenewalReminders() {
    // Tìm thẻ hết hạn trong 7 ngày
    // Gửi email có link gia hạn nhanh
}
```

#### 2. Tạo endpoint gia hạn nhanh
```java
@GetMapping("/renew-quick/{passId}/{token}")
public String quickRenew(...) {
    // Verify token
    // Tạo đơn gia hạn
    // Redirect đến trang thanh toán
}
```

#### 3. Email template
- Tiêu đề: "🔔 Thẻ tháng sắp hết hạn"
- Nội dung: Thông tin thẻ + Nút "Gia hạn ngay"
- Link: 1-click đến trang thanh toán

---

## 📝 Checklist implement Auto-renewal (Cách 3)

- [ ] Tạo bảng `renewal_tokens` (lưu token gia hạn)
- [ ] Thêm Scheduler gửi email nhắc nhở (7 ngày trước)
- [ ] Tạo email template "Thẻ sắp hết hạn"
- [ ] Endpoint `/renew-quick/{token}` tự động tạo đơn
- [ ] (Tùy chọn) Gửi SMS nhắc nhở
- [ ] (Tùy chọn) Thông báo trong app/web

---

## 🚀 Tóm tắt

### **Hiện tại:**
- ❌ KHÔNG có tự động gia hạn
- ✅ Có gia hạn thủ công (Admin)
- ✅ Có scheduler đánh dấu EXPIRED

### **Để có tự động gia hạn:**
- **Cách đơn giản:** Gửi email nhắc + link gia hạn nhanh
- **Cách nâng cao:** Lưu thẻ + tự động charge (cần Payment Gateway)

### **Khuyến nghị:**
Implement Cách 3 (Nhắc nhở + Link nhanh) vì:
- Dễ làm nhất
- Chi phí thấp
- Trải nghiệm tốt

Bạn muốn implement tính năng này không?

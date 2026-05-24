# 🚀 QR Code Quick Start Guide

## 📋 Hướng dẫn nhanh sử dụng tính năng QR Code

---

## 👤 Dành cho KHÁCH HÀNG

### **Bước 1: Đặt vé hoặc đăng ký thẻ tháng**

**Đặt vé:**
```
1. Vào: http://localhost:8080/booking
2. Điền thông tin (tên, biển số, loại xe, thời gian)
3. Nhấn "Đặt vé"
```

**Đăng ký thẻ tháng:**
```
1. Vào: http://localhost:8080/the-thang
2. Điền thông tin (chủ xe, biển số, loại xe)
3. Nhấn "Đăng ký"
```

### **Bước 2: Thanh toán**

```
1. Trang chi tiết hiển thị QR thanh toán VietQR
2. Quét bằng app ngân hàng
3. Chuyển khoản
4. Hoặc nhấn "test-pay" để test
```

### **Bước 3: Nhận QR Code**

```
✅ Sau khi thanh toán thành công
✅ Trang tự động refresh
✅ QR code vé/thẻ hiển thị
✅ Chụp màn hình hoặc nhấn "Tải QR Code"
```

### **Bước 4: Đến bãi xe**

```
1. Mở ảnh QR code đã lưu
2. Xuất trình cho nhân viên
3. Nhân viên quét QR
4. Được phép vào bãi
```

---

## 👨‍💼 Dành cho NHÂN VIÊN

### **Bước 1: Mở trang quét QR**

```
URL: http://localhost:8080/staff/scan-qr

Hoặc trên production:
https://your-domain.com/staff/scan-qr
```

### **Bước 2: Cho phép camera**

```
1. Trình duyệt hỏi quyền camera
2. Nhấn "Cho phép"
3. Camera tự động bật
```

### **Bước 3: Quét QR của khách**

```
1. Khách xuất trình QR code
2. Đưa QR vào khung quét (250x250px)
3. Hệ thống tự động detect
4. Kết quả hiển thị ngay lập tức
```

### **Bước 4: Xem kết quả**

**✅ Nếu hợp lệ (màu xanh):**
```
- Hiển thị: ✅ Vé/Thẻ hợp lệ
- Thông tin: Mã, biển số, trạng thái, loại xe
- Nút: "✓ Cho xe vào"
```

**❌ Nếu không hợp lệ (màu đỏ):**
```
- Hiển thị: ❌ Lý do (vé hết hạn, chưa thanh toán, v.v.)
- Tự động reset sau 3 giây
- Sẵn sàng quét QR tiếp theo
```

### **Bước 5: Xác nhận**

```
1. Nhấn "✓ Cho xe vào"
2. Hiển thị: "✅ Đã xác nhận!"
3. Tự động reset sau 2 giây
4. Sẵn sàng quét QR tiếp theo
```

---

## 🧪 Test nhanh

### **Test 1: Vé đặt trước**

```bash
# 1. Tạo vé
curl -X POST http://localhost:8080/dat-ve \
  -d "customerName=Test User" \
  -d "licensePlate=30A12345" \
  -d "vehicleType=xe_may" \
  -d "checkIn=2026-05-24T08:00" \
  -d "checkOut=2026-05-24T20:00"

# 2. Lấy ID từ response, vào trang chi tiết
http://localhost:8080/ve/{id}

# 3. Nhấn "test-pay"

# 4. Xem QR code hiển thị

# 5. Quét QR tại:
http://localhost:8080/staff/scan-qr
```

### **Test 2: Thẻ tháng**

```bash
# 1. Đăng ký thẻ
curl -X POST http://localhost:8080/dang-ky-the-thang \
  -d "ownerName=Test User" \
  -d "licensePlate=51B67890" \
  -d "vehicleType=o_to" \
  -d "startDate=2026-05-24" \
  -d "months=1"

# 2. Vào trang chi tiết
http://localhost:8080/the-thang/{id}

# 3. Nhấn "test-pay"

# 4. Xem QR code hiển thị

# 5. Quét QR
```

### **Test 3: API Verify**

```bash
# Test booking
curl -X POST http://localhost:8080/api/qr/verify \
  -H "Content-Type: application/json" \
  -d '{"qrData": "SMARTPARK_BOOKING|1|SP123456|30A12345|2026-05-24|A-01"}'

# Test monthly pass
curl -X POST http://localhost:8080/api/qr/verify \
  -H "Content-Type: application/json" \
  -d '{"qrData": "SMARTPARK_PASS|1|MP123456|51B67890|2026-06-24"}'
```

---

## 🔧 Troubleshooting

### **Vấn đề: QR không hiển thị**

**Nguyên nhân:**
- Vé/thẻ chưa thanh toán

**Giải pháp:**
```
1. Check status trong database
2. Phải là PAID/CONFIRMED (vé) hoặc ACTIVE (thẻ)
3. Nhấn "test-pay" để test thanh toán
```

### **Vấn đề: Camera không bật**

**Nguyên nhân:**
- Trình duyệt chặn quyền camera
- Chạy trên HTTP (không phải HTTPS/localhost)

**Giải pháp:**
```
1. Cho phép camera trong browser settings
2. Chạy trên localhost hoặc HTTPS
3. Hoặc dùng chức năng "Nhập mã thủ công"
```

### **Vấn đề: Scanner không detect QR**

**Nguyên nhân:**
- QR code quá nhỏ/mờ
- Ánh sáng không đủ

**Giải pháp:**
```
1. Phóng to QR code
2. Tăng độ sáng màn hình
3. Đưa QR gần camera hơn
```

### **Vấn đề: API trả về lỗi**

**Nguyên nhân:**
- QR data sai format
- ID không tồn tại
- Status không hợp lệ

**Giải pháp:**
```
1. Check console log backend
2. Verify QR data format
3. Check database có record không
```

---

## 📱 Mobile Usage

### **Khách hàng (Mobile):**

```
1. Đặt vé/đăng ký thẻ trên mobile browser
2. Thanh toán
3. Chụp màn hình QR code
4. Lưu vào thư viện ảnh
5. Đến bãi xe → Mở ảnh → Xuất trình
```

### **Nhân viên (Mobile):**

```
1. Mở: https://your-domain.com/staff/scan-qr
2. Cho phép camera
3. Camera sau tự động bật
4. Quét QR của khách
5. Xem kết quả full screen
6. Nhấn "Cho xe vào"
```

**Lưu ý:**
- Phải chạy trên HTTPS (production)
- Camera sau tự động được chọn (facingMode: environment)
- Layout responsive, dễ sử dụng trên mobile

---

## 📊 QR Code Format

### **Vé đặt trước:**
```
SMARTPARK_BOOKING|{id}|{code}|{plate}|{date}|{slot}

Ví dụ:
SMARTPARK_BOOKING|123|SP123456|30A12345|2026-05-24|A-01
```

### **Thẻ tháng:**
```
SMARTPARK_PASS|{id}|{code}|{plate}|{endDate}

Ví dụ:
SMARTPARK_PASS|456|MP123456|51B67890|2026-06-24
```

---

## ✅ Validation Rules

### **Vé đặt trước:**
```
✅ Vé phải tồn tại
✅ Mã code phải khớp
✅ Status = PAID hoặc CONFIRMED
✅ Ngày đặt >= hôm nay
```

### **Thẻ tháng:**
```
✅ Thẻ phải tồn tại
✅ Mã code phải khớp
✅ Status = ACTIVE
✅ Ngày hết hạn >= hôm nay
```

---

## 🎯 URLs Quan trọng

### **Khách hàng:**
```
Đặt vé:           /booking
Đăng ký thẻ:      /the-thang
Chi tiết vé:      /ve/{id}
Chi tiết thẻ:     /the-thang/{id}
```

### **Nhân viên:**
```
Quét QR:          /staff/scan-qr
Dashboard:        /dashboard
```

### **API:**
```
Verify QR:        POST /api/qr/verify
Check status:     GET /api/status/{id}
Webhook:          POST /api/webhook/payment-status
```

---

## 📚 Tài liệu đầy đủ

Xem thêm:
- `PHASE-3-SUMMARY.md` - Tổng kết Phase 3
- `QR-INTEGRATION-TEST-GUIDE.md` - Hướng dẫn test chi tiết
- `HOW-QR-VERIFICATION-WORKS.md` - Giải thích kỹ thuật
- `QR-FLOW-DIAGRAM.md` - Sơ đồ flow

---

## 🆘 Support

Nếu gặp vấn đề:
1. Check console log (F12)
2. Check backend log
3. Xem tài liệu troubleshooting
4. Test API bằng curl

---

**Status:** Quick Start Guide  
**Version:** 1.0  
**Last Updated:** 2026-05-24

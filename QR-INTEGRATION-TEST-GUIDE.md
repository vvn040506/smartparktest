# 🧪 Hướng dẫn Test tích hợp QR Code

## 📋 Tổng quan

Tài liệu này hướng dẫn cách test đầy đủ tính năng QR Code đã tích hợp trong Phase 3.

---

## ✅ Checklist Test

### **1. Test QR Code cho Vé đặt trước**
- [ ] Tạo booking mới
- [ ] Thanh toán (test-pay)
- [ ] Xem QR code hiển thị trên trang chi tiết
- [ ] Tải QR code về máy
- [ ] Quét QR bằng trang staff scanner
- [ ] Verify thông tin hiển thị đúng

### **2. Test QR Code cho Thẻ tháng**
- [ ] Đăng ký thẻ tháng mới
- [ ] Thanh toán (test-pay)
- [ ] Xem QR code hiển thị trên trang chi tiết
- [ ] Tải QR code về máy
- [ ] Quét QR bằng trang staff scanner
- [ ] Verify thông tin hiển thị đúng

### **3. Test Scanner Page**
- [ ] Mở trang /staff/scan-qr
- [ ] Cho phép camera access
- [ ] Quét QR hợp lệ → Hiển thị màu xanh
- [ ] Quét QR không hợp lệ → Hiển thị màu đỏ
- [ ] Test nút "Cho xe vào"
- [ ] Test auto-resume scanner

### **4. Test Validation**
- [ ] QR vé chưa thanh toán → Reject
- [ ] QR vé đã hết hạn → Reject
- [ ] QR thẻ tháng hết hạn → Reject
- [ ] QR thẻ tháng không ACTIVE → Reject
- [ ] QR code sai format → Reject

---

## 🧪 Test Case 1: Vé đặt trước (Booking)

### **Bước 1: Tạo vé**
```
1. Mở: http://localhost:8080/booking
2. Điền form:
   - Tên: Nguyễn Văn A
   - Biển số: 30A12345
   - Loại xe: Xe máy
   - Vị trí: A-01
   - Check-in: 2026-05-24 08:00
   - Check-out: 2026-05-24 20:00
3. Nhấn "Đặt vé"
```

### **Bước 2: Thanh toán**
```
1. Trang chi tiết vé hiển thị
2. Nhấn nút "test-pay" (webhook test)
3. Trang tự động refresh
4. Status chuyển sang "PAID"
```

### **Bước 3: Xem QR Code**
```
✅ Phải thấy section "🎫 QR Code vé của bạn"
✅ QR code hiển thị (250x250px)
✅ Có nút "💾 Tải QR Code"
✅ Text hướng dẫn: "Xuất trình khi vào bãi xe"
```

### **Bước 4: Tải QR Code**
```
1. Nhấn nút "💾 Tải QR Code"
2. File PNG tự động download
3. Tên file: ve-SP123456.png (SP + paymentCode)
4. Mở file → Xem QR code rõ ràng
```

### **Bước 5: Quét QR**
```
1. Mở: http://localhost:8080/staff/scan-qr
2. Cho phép camera (nếu hỏi)
3. Đưa QR code vào camera (hoặc mở file PNG trên màn hình khác)
4. Scanner tự động detect và gửi verify
```

### **Bước 6: Verify kết quả**
```
✅ Hiển thị box màu xanh
✅ Icon: ✅ (checkmark lớn)
✅ Tiêu đề: "🎫 Vé đặt trước hợp lệ!"
✅ Thông tin:
   - Mã: SP123456
   - Biển số: 30A12345 (font lớn)
   - Trạng thái: PAID (màu xanh)
   - Khách hàng: Nguyễn Văn A
   - Loại xe: 🏍️ Xe máy
   - Vị trí: A-01
✅ Nút "✓ Cho xe vào" (màu xanh, full width)
✅ Nút "Quét QR khác" (outline)
```

### **Bước 7: Xác nhận**
```
1. Nhấn "✓ Cho xe vào"
2. Hiển thị: "✅ Đã xác nhận! Xe được phép vào."
3. Sau 2 giây → Scanner reset tự động
4. Sẵn sàng quét QR tiếp theo
```

---

## 🧪 Test Case 2: Thẻ tháng (Monthly Pass)

### **Bước 1: Đăng ký thẻ**
```
1. Mở: http://localhost:8080/the-thang
2. Điền form:
   - Chủ xe: Trần Thị B
   - Email: tran.b@example.com
   - Biển số: 51B67890
   - Loại xe: Ô tô
   - Ngày bắt đầu: 2026-05-24
   - Số tháng: 1
3. Nhấn "Đăng ký"
```

### **Bước 2: Thanh toán**
```
1. Trang chi tiết thẻ hiển thị
2. Nhấn nút "test-pay"
3. Status chuyển sang "ACTIVE"
```

### **Bước 3: Xem QR Code**
```
✅ Phải thấy section "🎟️ QR Code thẻ tháng"
✅ QR code hiển thị (250x250px)
✅ Có nút "💾 Tải QR Code"
✅ Text: "Xuất trình khi ra vào bãi xe"
```

### **Bước 4: Quét QR**
```
1. Mở: http://localhost:8080/staff/scan-qr
2. Quét QR code thẻ tháng
```

### **Bước 5: Verify kết quả**
```
✅ Hiển thị box màu xanh
✅ Icon: ✅
✅ Tiêu đề: "🎟️ Thẻ tháng hợp lệ!"
✅ Thông tin:
   - Mã: MP123456
   - Biển số: 51B67890
   - Trạng thái: ACTIVE
   - Chủ xe: Trần Thị B
   - Loại xe: 🚗 Ô tô
   - Hết hạn: 24/06/2026
✅ Nút "✓ Cho xe vào"
```

---

## 🧪 Test Case 3: Validation (Negative Tests)

### **Test 3.1: Vé chưa thanh toán**
```
1. Tạo vé mới
2. KHÔNG thanh toán (để status = PENDING)
3. Tạo QR thủ công: SMARTPARK_BOOKING|{id}|{code}|30A12345|2026-05-24|A-01
4. Quét QR
5. ❌ Kết quả: "Vé chưa thanh toán (Status: PENDING)"
```

### **Test 3.2: Vé đã hết hạn**
```
1. Tạo vé với bookingDate = 2026-05-20 (quá khứ)
2. Thanh toán
3. Quét QR
4. ❌ Kết quả: "Vé đã hết hạn"
```

### **Test 3.3: Thẻ tháng hết hạn**
```
1. Tạo thẻ tháng với endDate = 2026-05-20 (quá khứ)
2. Quét QR
3. ❌ Kết quả: "Thẻ đã hết hạn"
```

### **Test 3.4: QR code sai format**
```
1. Tạo QR với data: "INVALID_FORMAT|123|456"
2. Quét QR
3. ❌ Kết quả: "QR code không hợp lệ"
```

### **Test 3.5: Mã code không khớp**
```
1. Tạo QR với code sai: SMARTPARK_BOOKING|1|WRONG_CODE|30A12345|2026-05-24|A-01
2. Quét QR
3. ❌ Kết quả: "Mã vé không khớp"
```

### **Test 3.6: ID không tồn tại**
```
1. Tạo QR với ID = 99999 (không tồn tại)
2. Quét QR
3. ❌ Kết quả: "Vé không tồn tại"
```

---

## 🧪 Test Case 4: API Testing

### **Test API Verify - Booking hợp lệ**
```bash
curl -X POST http://localhost:8080/api/qr/verify \
  -H "Content-Type: application/json" \
  -d '{
    "qrData": "SMARTPARK_BOOKING|1|SP123456|30A12345|2026-05-24|A-01"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Vé hợp lệ",
  "data": {
    "type": "booking",
    "id": 1,
    "code": "SP123456",
    "licensePlate": "30A12345",
    "status": "PAID",
    "details": {
      "customerName": "Nguyễn Văn A",
      "vehicleType": "xe_may",
      "slotId": "A-01",
      "bookingDate": "2026-05-24"
    }
  }
}
```

### **Test API Verify - Monthly Pass hợp lệ**
```bash
curl -X POST http://localhost:8080/api/qr/verify \
  -H "Content-Type: application/json" \
  -d '{
    "qrData": "SMARTPARK_PASS|1|MP123456|51B67890|2026-06-24"
  }'
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Thẻ tháng hợp lệ",
  "data": {
    "type": "monthly_pass",
    "id": 1,
    "code": "MP123456",
    "licensePlate": "51B67890",
    "status": "ACTIVE",
    "details": {
      "ownerName": "Trần Thị B",
      "vehicleType": "o_to",
      "endDate": "2026-06-24"
    }
  }
}
```

### **Test API Verify - QR không hợp lệ**
```bash
curl -X POST http://localhost:8080/api/qr/verify \
  -H "Content-Type: application/json" \
  -d '{
    "qrData": "INVALID_QR_DATA"
  }'
```

**Expected Response:**
```json
{
  "success": false,
  "message": "QR code không hợp lệ",
  "data": null
}
```

---

## 📱 Test trên Mobile

### **Chuẩn bị:**
```
1. Deploy app lên server có HTTPS (hoặc dùng ngrok)
2. Mở trình duyệt mobile (Chrome/Safari)
3. Vào: https://your-domain.com/staff/scan-qr
```

### **Test camera:**
```
✅ Trình duyệt hỏi quyền camera → Cho phép
✅ Camera sau tự động bật (facingMode: environment)
✅ Khung quét hiển thị (250x250px)
✅ Đưa QR code vào khung → Tự động detect
✅ Hiển thị kết quả full screen
✅ Nút "Cho xe vào" dễ nhấn (large button)
```

### **Test responsive:**
```
✅ Layout mobile-friendly
✅ QR code hiển thị vừa màn hình
✅ Text đọc được (không quá nhỏ)
✅ Nút đủ lớn để nhấn
✅ Không bị scroll ngang
```

---

## 🐛 Troubleshooting

### **Vấn đề 1: QR không hiển thị**
```
Nguyên nhân:
- Vé/thẻ chưa thanh toán (status != PAID/ACTIVE)
- Lỗi generate QR (check console log)

Giải pháp:
1. Check status trong database
2. Check console log: "Lỗi tạo QR vé: ..."
3. Verify ZXing dependency trong pom.xml
```

### **Vấn đề 2: Camera không bật**
```
Nguyên nhân:
- Trình duyệt chặn quyền camera
- Chạy trên HTTP (không phải HTTPS/localhost)
- Device không có camera

Giải pháp:
1. Cho phép camera trong browser settings
2. Chạy trên localhost hoặc HTTPS
3. Dùng chức năng "Nhập mã thủ công"
```

### **Vấn đề 3: Scanner không detect QR**
```
Nguyên nhân:
- QR code quá nhỏ/mờ
- Ánh sáng không đủ
- QR code bị hỏng

Giải pháp:
1. Phóng to QR code
2. Tăng độ sáng màn hình
3. Tải lại QR code mới
4. Dùng chức năng nhập mã thủ công
```

### **Vấn đề 4: API verify trả về lỗi**
```
Nguyên nhân:
- QR data sai format
- ID không tồn tại
- Status không hợp lệ

Giải pháp:
1. Check QR data format
2. Check database có record không
3. Check status field
4. Check console log backend
```

---

## ✅ Acceptance Criteria

### **Phase 3 được coi là hoàn thành khi:**

- [x] QR code hiển thị đúng trên trang chi tiết vé (sau thanh toán)
- [x] QR code hiển thị đúng trên trang chi tiết thẻ tháng (khi ACTIVE)
- [x] Nút tải QR code hoạt động (download PNG)
- [x] Trang /staff/scan-qr hiển thị đúng
- [x] Camera scanner hoạt động (HTML5 QR Code)
- [x] API /api/qr/verify hoạt động đúng
- [x] Verify booking: Check status, date, code
- [x] Verify pass: Check status, endDate, code
- [x] Hiển thị kết quả đúng (màu xanh/đỏ, thông tin chi tiết)
- [x] Nút "Cho xe vào" hoạt động
- [x] Auto-resume scanner sau khi xác nhận
- [x] Responsive trên mobile
- [x] Error handling đầy đủ

---

## 📊 Test Results Template

```
Date: ___________
Tester: ___________

Test Case 1: Vé đặt trước
[ ] Tạo vé: PASS / FAIL
[ ] Thanh toán: PASS / FAIL
[ ] Hiển thị QR: PASS / FAIL
[ ] Tải QR: PASS / FAIL
[ ] Quét QR: PASS / FAIL
[ ] Verify: PASS / FAIL
[ ] Xác nhận: PASS / FAIL

Test Case 2: Thẻ tháng
[ ] Đăng ký: PASS / FAIL
[ ] Thanh toán: PASS / FAIL
[ ] Hiển thị QR: PASS / FAIL
[ ] Tải QR: PASS / FAIL
[ ] Quét QR: PASS / FAIL
[ ] Verify: PASS / FAIL

Test Case 3: Validation
[ ] Vé chưa thanh toán: PASS / FAIL
[ ] Vé hết hạn: PASS / FAIL
[ ] Thẻ hết hạn: PASS / FAIL
[ ] QR sai format: PASS / FAIL
[ ] Mã không khớp: PASS / FAIL
[ ] ID không tồn tại: PASS / FAIL

Test Case 4: API
[ ] Verify booking: PASS / FAIL
[ ] Verify pass: PASS / FAIL
[ ] Invalid QR: PASS / FAIL

Mobile Test:
[ ] Camera: PASS / FAIL
[ ] Responsive: PASS / FAIL
[ ] UX: PASS / FAIL

Overall: PASS / FAIL
Notes: ___________
```

---

## 🎯 Next Steps

Sau khi Phase 3 test xong:
1. Fix bugs (nếu có)
2. Deploy lên staging
3. Test trên production-like environment
4. Bắt đầu Phase 4: My Account Page

---

**Status:** Phase 3 Testing Guide  
**Version:** 1.0  
**Last Updated:** 2026-05-24

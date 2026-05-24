# Test API Thẻ tháng - SmartPark

## 1. Tạo thẻ tháng mới

```bash
curl -X POST http://localhost:8080/api/monthly-passes \
  -H "Content-Type: application/json" \
  -d '{
    "ownerName": "Nguyễn Văn A",
    "email": "test@example.com",
    "licensePlate": "30A12345",
    "vehicleType": "xe_may",
    "startDate": "2026-05-23",
    "months": 1,
    "note": "Test thẻ tháng"
  }'
```

**Response mẫu:**
```json
{
  "success": true,
  "message": "Đã tạo thẻ tháng. Vui lòng thanh toán.",
  "data": {
    "id": 1,
    "paymentCode": "MP123456",
    "amountDue": 500000,
    "status": "PENDING",
    "startDate": "2026-05-23",
    "endDate": "2026-06-22"
  }
}
```

---

## 2. Lấy danh sách tất cả thẻ tháng

```bash
curl http://localhost:8080/api/monthly-passes
```

---

## 3. Lấy thông tin thẻ theo ID

```bash
curl http://localhost:8080/api/monthly-passes/1
```

---

## 4. Tìm thẻ theo biển số xe

```bash
curl http://localhost:8080/api/monthly-passes/plate/30A12345
```

---

## 5. Kiểm tra thẻ có hợp lệ không

```bash
curl http://localhost:8080/api/monthly-passes/check/30A12345
```

**Response nếu có thẻ hợp lệ:**
```json
{
  "success": true,
  "message": "Thẻ tháng hợp lệ",
  "data": {
    "id": 1,
    "licensePlate": "30A12345",
    "status": "ACTIVE",
    "endDate": "2026-06-22"
  }
}
```

---

## 6. Test thanh toán (giả lập webhook)

```bash
curl "http://localhost:8080/webhook/test-pay?code=MP123456&amount=500000"
```

**Response:**
```json
{
  "ok": true,
  "message": "Thanh toán thành công"
}
```

---

## 7. Admin: Kích hoạt thủ công (bỏ qua thanh toán)

```bash
curl -X POST http://localhost:8080/api/monthly-passes/1/activate
```

---

## 8. Admin: Hủy thẻ

```bash
curl -X POST http://localhost:8080/api/monthly-passes/1/cancel
```

---

## 9. Admin: Gia hạn thẻ

```bash
curl -X POST "http://localhost:8080/api/monthly-passes/1/renew?months=2"
```

---

## Test Scenarios

### Scenario 1: Đăng ký và thanh toán thành công
1. Tạo thẻ mới (API #1) → Nhận `paymentCode`
2. Test thanh toán (API #6) với `paymentCode` vừa nhận
3. Kiểm tra thẻ (API #5) → Status = ACTIVE

### Scenario 2: Kiểm tra xe có thẻ hợp lệ khi vào bãi
1. Tạo và kích hoạt thẻ cho biển số `30A12345`
2. Gọi API #5 để check → Trả về thẻ hợp lệ
3. Cho xe vào không cần đặt vé

### Scenario 3: Thẻ hết hạn
1. Tạo thẻ với `startDate` trong quá khứ
2. Đợi qua `endDate`
3. Gọi API #5 → Không tìm thấy thẻ hợp lệ

### Scenario 4: Gia hạn thẻ
1. Có thẻ ACTIVE
2. Gọi API #9 để gia hạn thêm 2 tháng
3. Kiểm tra `endDate` đã được cập nhật

---

## Giá thẻ tháng (mặc định trong code)

- **Xe máy:** 500,000 VNĐ/tháng
- **Ô tô:** 2,000,000 VNĐ/tháng

---

## Database: Xem dữ liệu thẻ tháng

Truy cập H2 Console: http://localhost:8080/h2-console

```sql
-- Xem tất cả thẻ tháng
SELECT * FROM monthly_passes;

-- Xem thẻ đang hoạt động
SELECT * FROM monthly_passes WHERE status = 'ACTIVE';

-- Xem thẻ theo biển số
SELECT * FROM monthly_passes WHERE license_plate = '30A12345';
```

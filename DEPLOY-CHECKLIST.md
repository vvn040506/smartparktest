# ✅ Checklist Deploy - Tính năng Thẻ tháng

## Trước khi deploy

- [ ] **Thay thông tin ngân hàng** trong `application.properties`:
  ```properties
  app.bank.account=YOUR_ACCOUNT_NUMBER
  app.bank.owner=YOUR_NAME
  app.bank.name=YOUR_BANK
  ```

- [ ] **Đổi mật khẩu admin** trong `DataInitializer.java`

- [ ] **Kiểm tra database config** có `ddl-auto=update` để tự tạo bảng `monthly_passes`

## Sau khi deploy

- [ ] **Đăng ký webhook SePay**:
  - URL: `https://your-domain.com/webhook/sepay`
  - Vào: https://my.sepay.vn → Cài đặt → Webhook

- [ ] **Test các trang:**
  - [ ] Trang chủ: `https://your-domain.com`
  - [ ] Trang thẻ tháng: `https://your-domain.com/the-thang`
  - [ ] Admin: `https://your-domain.com/admin`

- [ ] **Test flow đăng ký thẻ tháng:**
  1. Vào `/the-thang`
  2. Điền form đăng ký
  3. Nhận mã thanh toán (MP...)
  4. Test webhook: `/webhook/test-pay?code=MP123456&amount=500000`
  5. Kiểm tra thẻ đã ACTIVE

- [ ] **Kiểm tra database:**
  - [ ] Bảng `monthly_passes` đã được tạo
  - [ ] Dữ liệu test có trong bảng

## Giao diện đã có sẵn

✅ Trang đăng ký: `/the-thang` (file: `monthly-pass.html`)  
✅ Trang chi tiết + QR: `/the-thang/{id}` (file: `monthly-pass-detail.html`)  
✅ Link trong menu trang chủ: "📅 Đăng ký thẻ tháng →"  
✅ Admin dashboard: Quản lý thẻ tháng  

## API endpoints có sẵn

✅ `POST /api/monthly-passes` - Tạo thẻ mới  
✅ `GET /api/monthly-passes` - Danh sách thẻ  
✅ `GET /api/monthly-passes/{id}` - Chi tiết thẻ  
✅ `GET /api/monthly-passes/plate/{plate}` - Tìm theo biển số  
✅ `GET /api/monthly-passes/check/{plate}` - Kiểm tra thẻ hợp lệ  
✅ `POST /api/monthly-passes/{id}/activate` - Kích hoạt (admin)  
✅ `POST /api/monthly-passes/{id}/cancel` - Hủy (admin)  
✅ `POST /api/monthly-passes/{id}/renew` - Gia hạn (admin)  

## Webhook

✅ `POST /webhook/sepay` - Nhận thông báo từ SePay  
✅ `GET /webhook/test-pay` - Test thanh toán thủ công  

## Không cần làm gì thêm về code!

Tất cả tính năng thẻ tháng đã được implement đầy đủ:
- ✅ Model, Repository, Service, Controller
- ✅ Giao diện HTML (Thymeleaf)
- ✅ Webhook tự động kích hoạt
- ✅ QR code thanh toán VietQR
- ✅ Admin quản lý

**Chỉ cần deploy và cấu hình webhook SePay là xong!**

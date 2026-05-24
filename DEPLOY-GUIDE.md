# 🚀 Hướng dẫn Deploy SmartPark (có tính năng Thẻ tháng)

## ✅ Checklist trước khi deploy

### 1. **Cấu hình Database Production**
File: `src/main/resources/application.properties`

```properties
# Database (PostgreSQL trên Render/Supabase)
spring.datasource.url=jdbc:postgresql://your-db-host:5432/your-db-name
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=update
```

**Lưu ý:** Đảm bảo `ddl-auto=update` để tự động tạo bảng `monthly_passes`

---

### 2. **Biến môi trường cần thiết**

Khi deploy trên Render/Heroku/Railway, cần set các biến môi trường:

| Biến | Mô tả | Ví dụ |
|------|-------|-------|
| `DB_USERNAME` | Username database | `postgres` |
| `DB_PASSWORD` | Password database | `your-secure-password` |
| `RESEND_API_KEY` | API key gửi email (tùy chọn) | `re_xxx...` |
| `PORT` | Port server (tự động) | `8080` |

---

### 3. **Cấu hình thông tin ngân hàng**

File: `src/main/resources/application.properties`

```properties
# Thông tin tài khoản nhận tiền
app.bank.account=0707561652
app.bank.owner=NGUYEN THANH HUY
app.bank.name=MB
```

**⚠️ QUAN TRỌNG:** Thay bằng thông tin tài khoản thật của bạn!

---

### 4. **Đăng ký Webhook SePay**

Sau khi deploy xong, bạn sẽ có URL production (ví dụ: `https://smartpark-abc.onrender.com`)

#### Bước 1: Đăng nhập SePay
- Truy cập: https://my.sepay.vn
- Đăng nhập tài khoản

#### Bước 2: Cấu hình Webhook
- Vào **Cài đặt** → **Webhook**
- Điền URL: `https://smartpark-abc.onrender.com/webhook/sepay`
- Lưu lại

#### Bước 3: Test Webhook
```bash
curl -X POST https://smartpark-abc.onrender.com/webhook/sepay \
  -H "Content-Type: application/json" \
  -d '{
    "content": "MP123456",
    "amount": 500000,
    "bankRef": "TEST123"
  }'
```

---

## 📦 Deploy lên Render

### Bước 1: Chuẩn bị code

```bash
cd smartparktest-master/smartparktest-master

# Tạo file .gitignore nếu chưa có
echo "target/" >> .gitignore
echo "*.log" >> .gitignore
echo "data/" >> .gitignore

# Commit code
git init
git add .
git commit -m "Deploy SmartPark with Monthly Pass feature"
```

### Bước 2: Push lên GitHub

```bash
# Tạo repo mới trên GitHub, sau đó:
git remote add origin https://github.com/YOUR_USERNAME/smartpark.git
git branch -M main
git push -u origin main
```

### Bước 3: Tạo Database trên Supabase (Free)

1. Truy cập: https://supabase.com
2. Tạo project mới
3. Vào **Settings** → **Database**
4. Copy **Connection String** (dạng: `postgresql://...`)

### Bước 4: Deploy trên Render

1. Truy cập: https://render.com
2. Nhấn **New** → **Web Service**
3. Kết nối GitHub repo vừa tạo
4. Cấu hình:

```yaml
Name: smartpark
Runtime: Java
Build Command: mvn clean package -DskipTests
Start Command: java -jar target/smartpark-0.0.1-SNAPSHOT.jar
Instance Type: Free
```

5. **Environment Variables:**
   - `DB_USERNAME`: `postgres`
   - `DB_PASSWORD`: (copy từ Supabase)
   - `DATABASE_URL`: (connection string từ Supabase)
   - `RESEND_API_KEY`: (nếu có)

6. Nhấn **Create Web Service**

### Bước 5: Đợi deploy (3-5 phút)

Render sẽ:
- Clone code từ GitHub
- Chạy `mvn clean package`
- Khởi động ứng dụng
- Cấp URL: `https://smartpark-xxx.onrender.com`

---

## 🧪 Test sau khi deploy

### 1. Kiểm tra trang chủ
```
https://smartpark-xxx.onrender.com
```

### 2. Kiểm tra trang thẻ tháng
```
https://smartpark-xxx.onrender.com/the-thang
```

### 3. Test đăng ký thẻ tháng
1. Vào trang `/the-thang`
2. Điền form đăng ký
3. Nhận mã thanh toán (ví dụ: `MP123456`)
4. Chuyển khoản với nội dung = mã thanh toán
5. Webhook tự động kích hoạt thẻ

### 4. Test webhook thủ công
```bash
curl "https://smartpark-xxx.onrender.com/webhook/test-pay?code=MP123456&amount=500000"
```

---

## 🔧 Cấu hình nâng cao

### Thay đổi giá thẻ tháng

File: `src/main/java/com/smartpark/service/impl/MonthlyPassServiceImpl.java`

```java
private long calculateAmount(String vehicleType, int months) {
    long pricePerMonth = vehicleType.equals("o_to") 
        ? 2_000_000L  // Ô tô: 2 triệu/tháng
        : 500_000L;   // Xe máy: 500k/tháng
    return pricePerMonth * months;
}
```

### Thêm link vào menu

File: `src/main/resources/templates/index.html`

```html
<nav>
  <a href="/">Trang chủ</a>
  <a href="/booking">Đặt vé</a>
  <a href="/the-thang">Đăng ký thẻ tháng</a>
  <a href="/login">Đăng nhập</a>
</nav>
```

---

## 🐛 Troubleshooting

### Lỗi: Không tạo được bảng `monthly_passes`

**Nguyên nhân:** Database chưa có bảng

**Giải pháp:**
1. Kiểm tra `spring.jpa.hibernate.ddl-auto=update`
2. Hoặc tạo bảng thủ công:

```sql
CREATE TABLE monthly_passes (
    id BIGSERIAL PRIMARY KEY,
    owner_name VARCHAR(255),
    email VARCHAR(255),
    license_plate VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(50),
    amount_due BIGINT,
    payment_code VARCHAR(50) UNIQUE,
    paid_at TIMESTAMP,
    bank_ref VARCHAR(255),
    note TEXT,
    created_at TIMESTAMP
);
```

### Lỗi: Webhook không hoạt động

**Kiểm tra:**
1. URL webhook đúng chưa: `https://your-domain.com/webhook/sepay`
2. SePay đã cấu hình webhook chưa
3. Xem log trên Render: **Logs** tab

### Lỗi: Không hiển thị trang thẻ tháng

**Kiểm tra:**
1. File `monthly-pass.html` có trong `src/main/resources/templates/`
2. Controller có route `/the-thang`
3. Build lại: `mvn clean package`

---

## 📊 Quản lý thẻ tháng (Admin)

### Truy cập Admin Dashboard
```
https://smartpark-xxx.onrender.com/admin
```

### Chức năng Admin:
- ✅ Xem danh sách tất cả thẻ tháng
- ✅ Kích hoạt thẻ thủ công (không cần thanh toán)
- ✅ Hủy thẻ
- ✅ Gia hạn thẻ

---

## 🔐 Bảo mật

### 1. Đổi mật khẩu admin mặc định

File: `src/main/java/com/smartpark/DataInitializer.java`

```java
// Tạo admin account
StaffAccount admin = new StaffAccount();
admin.setUsername("admin");
admin.setPassword(passwordEncoder.encode("your-strong-password")); // ĐỔI MẬT KHẨU
admin.setRole("ADMIN");
```

### 2. Bật HTTPS

Render tự động cấp SSL certificate miễn phí.

### 3. Giới hạn rate limit

Thêm dependency:

```xml
<dependency>
    <groupId>com.github.vladimir-bukhtoyarov</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.1.0</version>
</dependency>
```

---

## 📈 Monitoring

### Xem logs trên Render
1. Vào Dashboard → Your Service
2. Tab **Logs**
3. Xem real-time logs

### Xem database
1. Truy cập Supabase Dashboard
2. **Table Editor** → `monthly_passes`
3. Xem dữ liệu thẻ tháng

---

## 🎯 Tóm tắt

✅ **Đã có sẵn:**
- ✅ Giao diện đăng ký thẻ tháng (`/the-thang`)
- ✅ Trang chi tiết + QR thanh toán
- ✅ Webhook tự động kích hoạt
- ✅ Admin quản lý thẻ
- ✅ API đầy đủ

🚀 **Cần làm khi deploy:**
1. Cấu hình database production
2. Set biến môi trường
3. Deploy lên Render
4. Đăng ký webhook SePay
5. Test thử đăng ký thẻ

📞 **Hỗ trợ:**
- Xem log lỗi trên Render
- Check database trên Supabase
- Test webhook: `/webhook/test-pay`

# 📊 Phân tích: Hệ thống tài khoản User

## Hiện trạng

### ✅ Có sẵn:

#### 1. **Model User** (`User.java`)
```java
@Entity @Table(name = "users")
public class User {
    private Long id;
    private String username;  // unique
    private String email;     // unique
    private String password;  // BCrypt
    private LocalDateTime createdAt;
}
```

#### 2. **Chức năng đăng ký/đăng nhập**
- ✅ `POST /register` - Đăng ký tài khoản
- ✅ `POST /login` - Đăng nhập
- ✅ `GET /logout` - Đăng xuất
- ✅ Quên mật khẩu + Reset password

#### 3. **UserService**
- ✅ `register()` - Tạo tài khoản mới
- ✅ `login()` - Xác thực đăng nhập
- ✅ `sendResetLink()` - Gửi email reset password
- ✅ `resetPassword()` - Đặt lại mật khẩu

---

## ❌ Vấn đề: User và Thẻ tháng KHÔNG liên kết

### Hiện tại:
```java
// MonthlyPass.java
public class MonthlyPass {
    private String ownerName;      // Chỉ lưu tên (String)
    private String email;          // Chỉ lưu email (String)
    private String licensePlate;
    // KHÔNG có userId hoặc @ManyToOne User
}
```

### Nghĩa là:
- ❌ User đăng ký thẻ tháng **KHÔNG CẦN** đăng nhập
- ❌ Không lưu thông tin user nào tạo thẻ
- ❌ User không thể xem lịch sử thẻ của mình
- ❌ Không có trang "Thẻ của tôi"

---

## 🎯 Hai hướng xử lý

### **Hướng 1: Giữ nguyên (Khách vãng lai)**

**Ưu điểm:**
- ✅ Đơn giản, không cần đăng nhập
- ✅ Khách điền form là đăng ký được ngay
- ✅ Phù hợp với mô hình "đặt nhanh"

**Nhược điểm:**
- ❌ Khách không quản lý được thẻ của mình
- ❌ Không có lịch sử giao dịch
- ❌ Phải nhớ link chi tiết thẻ

**Khi nào dùng:**
- Bãi xe công cộng, khách lẻ
- Không cần quản lý user

---

### **Hướng 2: Liên kết User với MonthlyPass**

**Thay đổi cần làm:**

#### 1. Sửa Model MonthlyPass
```java
@Entity
public class MonthlyPass {
    // ... các field cũ
    
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;  // Thêm quan hệ với User
    
    // Giữ lại ownerName, email cho trường hợp admin tạo thủ công
}
```

#### 2. Thêm Controller cho User
```java
@GetMapping("/my-passes")
public String myPasses(HttpSession session, Model model) {
    User user = (User) session.getAttribute("user");
    if (user == null) return "redirect:/login";
    
    List<MonthlyPass> passes = monthlyPassService.findByUser(user.getId());
    model.addAttribute("passes", passes);
    return "my-passes";
}
```

#### 3. Sửa flow đăng ký thẻ
```java
@PostMapping("/dang-ky-the-thang")
public String dangKyTheThang(..., HttpSession session) {
    User user = (User) session.getAttribute("user");
    
    // Nếu chưa đăng nhập → yêu cầu đăng nhập
    if (user == null) {
        return "redirect:/login?returnUrl=/the-thang";
    }
    
    // Tạo thẻ và gắn với user
    MonthlyPass pass = monthlyPassService.create(req, user);
    return "redirect:/the-thang/" + pass.getId();
}
```

#### 4. Tạo trang "Thẻ của tôi"
- URL: `/my-passes`
- Hiển thị tất cả thẻ của user
- Trạng thái: PENDING, ACTIVE, EXPIRED
- Link thanh toán cho thẻ PENDING

**Ưu điểm:**
- ✅ User quản lý được thẻ của mình
- ✅ Xem lịch sử giao dịch
- ✅ Gia hạn thẻ dễ dàng
- ✅ Chuyên nghiệp hơn

**Nhược điểm:**
- ❌ Phải đăng nhập mới đăng ký được
- ❌ Phức tạp hơn cho khách

---

## 💡 Khuyến nghị: Hybrid (Kết hợp cả 2)

### Cho phép cả 2 luồng:

#### **Luồng 1: Khách vãng lai (không đăng nhập)**
```
/the-thang → Điền form → Nhận link chi tiết → Thanh toán
```
- Không lưu `user_id`
- Chỉ lưu `ownerName`, `email`

#### **Luồng 2: User đã đăng nhập**
```
/login → /my-account → Đăng ký thẻ → Xem trong "Thẻ của tôi"
```
- Lưu `user_id`
- User có thể xem lại tất cả thẻ

### Code mẫu:
```java
@PostMapping("/dang-ky-the-thang")
public String dangKyTheThang(..., HttpSession session) {
    User user = (User) session.getAttribute("user");
    
    // Tạo thẻ (có thể có hoặc không có user)
    MonthlyPass pass = monthlyPassService.create(req, user); // user có thể null
    
    return "redirect:/the-thang/" + pass.getId();
}
```

```java
// Service
public MonthlyPass create(CreateMonthlyPassRequest req, User user) {
    MonthlyPass pass = new MonthlyPass();
    // ... set các field
    
    if (user != null) {
        pass.setUser(user);  // Gắn user nếu đã đăng nhập
    }
    
    return repo.save(pass);
}
```

---

## 🚀 Tóm tắt

### **Hiện tại:**
- ✅ Có model User + đăng ký/đăng nhập
- ❌ User và MonthlyPass **KHÔNG** liên kết
- ❌ Đăng ký thẻ **KHÔNG CẦN** đăng nhập

### **Nếu muốn User quản lý thẻ:**
1. Thêm `@ManyToOne User` vào `MonthlyPass`
2. Tạo trang "Thẻ của tôi" (`/my-passes`)
3. Yêu cầu đăng nhập khi đăng ký thẻ
4. Hoặc cho phép cả 2 luồng (có/không đăng nhập)

### **Nếu giữ nguyên:**
- Khách đăng ký thẻ không cần tài khoản
- Phù hợp với mô hình "đặt nhanh"
- Đơn giản nhưng thiếu tính năng quản lý

---

## 📝 File cần tạo nếu implement User quản lý thẻ

1. `my-passes.html` - Trang "Thẻ của tôi"
2. `register.html` - Trang đăng ký (hiện chưa có template)
3. Sửa `MonthlyPass.java` - Thêm quan hệ User
4. Sửa `MonthlyPassService` - Thêm method `findByUser()`
5. Thêm route `/my-passes` trong Controller

Bạn muốn implement hướng nào?

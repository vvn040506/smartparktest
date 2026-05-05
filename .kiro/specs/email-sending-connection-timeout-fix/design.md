# Bugfix Design Document

## Root Cause Analysis

Sau khi phân tích code và lỗi, xác định được **root cause chính**:

### 1. Gmail App Password bị vô hiệu hóa (PRIMARY ROOT CAUSE)
- **Evidence**: User xác nhận email đã hoạt động trước đây nhưng ngừng hoạt động sau khi rollback commits
- **Root Cause**: Credentials (`gvofejaenxpsylmo`) bị hardcode trong `application.properties` → bị commit lên GitHub → Gmail phát hiện leak và tự động vô hiệu hóa App Password
- **Impact**: Tất cả SMTP connections đều thất bại với timeout (Gmail từ chối kết nối khi credentials không hợp lệ)

### 2. Biến môi trường production có default rỗng
- **Evidence**: `application-prod.properties` có `${MAIL_USERNAME:}` và `${MAIL_PASSWORD:}` với default là empty string
- **Root Cause**: Nếu env vars không được set trên Render, hệ thống sử dụng empty string → SMTP authentication thất bại
- **Impact**: Connection timeout hoặc authentication failure

### 3. Exception bị nuốt im lặng
- **Evidence**: Tất cả methods trong `EmailService.java` catch `MessagingException` và chỉ `System.err.println`
- **Root Cause**: Controller không biết email thất bại, vẫn trả về "thành công" cho user
- **Impact**: User không biết email không được gửi, debugging khó khăn

### 4. Port configuration không nhất quán
- **Evidence**: Dev dùng port 587 + STARTTLS, prod đã chuyển sang 465 + SSL nhưng env var trên Render có thể chưa update
- **Root Cause**: Biến môi trường `MAIL_PORT` trên Render có thể vẫn là 587
- **Impact**: Connection timeout nếu port không đúng

### 5. Gửi email trong @Transactional context (MINOR)
- **Evidence**: Một số methods có `@Transactional` và gọi email service
- **Root Cause**: SMTP call chậm → giữ DB connection lâu → có thể cạn kiệt connection pool
- **Impact**: Ảnh hưởng performance, không phải root cause chính của timeout

## Fix Strategy

### Phase 1: Immediate Fixes (Critical)
1. **Xóa credentials khỏi application.properties** (ngăn leak tiếp theo)
2. **Loại bỏ default values rỗng** trong application-prod.properties
3. **Throw exceptions thay vì nuốt** trong EmailService
4. **Chuẩn hóa SMTP config** (port 465 + SSL)

### Phase 2: Environment Setup (Critical)
5. **Hướng dẫn user update Render env vars**:
   - `MAIL_USERNAME=vovietnhat1996@gmail.com`
   - `MAIL_PASSWORD=qjzkpjotkanjbjsa` (App Password mới)
   - `MAIL_HOST=smtp.gmail.com`
   - `MAIL_PORT=465`

### Phase 3: Code Improvements (Important)
6. **Tách email sending ra khỏi @Transactional** (đã có @Async, cần verify)
7. **Thêm logging chi tiết** để debug dễ hơn
8. **Thêm validation** cho email configuration khi startup

## Technical Design

### 1. Remove Hardcoded Credentials

**File**: `src/main/resources/application.properties`

**Current**:
```properties
spring.mail.username=vovietnhat1996@gmail.com
spring.mail.password=gvofejaenxpsylmo
```

**Fixed**:
```properties
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

**Rationale**: Không hardcode credentials, bắt buộc phải set env vars

---

### 2. Remove Default Empty Values

**File**: `src/main/resources/application-prod.properties`

**Current**:
```properties
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
```

**Fixed**:
```properties
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
```

**Rationale**: Nếu env vars không được set, app sẽ fail-fast khi startup thay vì chạy với credentials rỗng

---

### 3. Throw Exceptions Instead of Swallowing

**File**: `src/main/java/com/smartpark/service/EmailService.java`

**Current** (tất cả methods):
```java
try {
    // ... send email
    mailSender.send(msg);
} catch (MessagingException e) {
    System.err.println("Lỗi gửi email: " + e.getMessage());
}
```

**Fixed** (tất cả methods):
```java
try {
    // ... send email
    mailSender.send(msg);
    System.out.println("✓ Email sent successfully to: " + toEmail);
} catch (MessagingException e) {
    System.err.println("✗ Failed to send email to " + toEmail + ": " + e.getMessage());
    throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
}
```

**Methods to update**:
- `sendResetEmail()`
- `sendAccountVerificationEmail()`
- `sendStaffResetEmail()`
- `sendStaffResetOTP()`
- `sendAccountVerificationOTP()`

**Rationale**: 
- Controller có thể catch exception và thông báo lỗi cho user
- Dễ debug hơn khi có stack trace đầy đủ
- Không che giấu lỗi thực sự

---

### 4. Standardize SMTP Configuration

**File**: `src/main/resources/application.properties`

**Current**:
```properties
spring.mail.port=587
spring.mail.properties.mail.smtp.starttls.enable=true
```

**Fixed**:
```properties
spring.mail.port=${MAIL_PORT:465}
spring.mail.properties.mail.smtp.starttls.enable=false
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
```

**File**: `src/main/resources/application-prod.properties`

**Already correct** (no changes needed):
```properties
spring.mail.port=${MAIL_PORT:465}
spring.mail.properties.mail.smtp.starttls.enable=false
spring.mail.properties.mail.smtp.ssl.enable=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
```

**Rationale**: 
- Port 465 + SSL là recommended cho Gmail
- Nhất quán giữa dev và prod
- Giảm confusion khi debug

---

### 5. Add Email Configuration Validation

**New File**: `src/main/java/com/smartpark/config/EmailConfigValidator.java`

```java
package com.smartpark.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EmailConfigValidator {

    @Value("${spring.mail.username:#{null}}")
    private String mailUsername;

    @Value("${spring.mail.password:#{null}}")
    private String mailPassword;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @EventListener(ApplicationReadyEvent.class)
    public void validateEmailConfig() {
        System.out.println("=== Email Configuration Validation ===");
        
        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            System.err.println("⚠️  WARNING: MAIL_USERNAME is not set or empty!");
        } else {
            System.out.println("✓ MAIL_USERNAME: " + maskEmail(mailUsername));
        }
        
        if (mailPassword == null || mailPassword.trim().isEmpty()) {
            System.err.println("⚠️  WARNING: MAIL_PASSWORD is not set or empty!");
        } else {
            System.out.println("✓ MAIL_PASSWORD: " + maskPassword(mailPassword));
        }
        
        System.out.println("✓ MAIL_HOST: " + mailHost);
        System.out.println("✓ MAIL_PORT: " + mailPort);
        
        if ((mailUsername == null || mailUsername.trim().isEmpty()) || 
            (mailPassword == null || mailPassword.trim().isEmpty())) {
            System.err.println("⚠️  Email functionality will NOT work without valid credentials!");
        } else {
            System.out.println("✓ Email configuration looks valid");
        }
        
        System.out.println("=====================================");
    }
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        return parts[0].substring(0, Math.min(3, parts[0].length())) + "***@" + parts[1];
    }
    
    private String maskPassword(String password) {
        if (password == null || password.length() < 4) return "***";
        return password.substring(0, 4) + "***";
    }
}
```

**Rationale**:
- Validate email config khi app startup
- Cảnh báo sớm nếu env vars thiếu
- Giúp debug dễ hơn trên production

---

### 6. Verify @Async Email Sending

**File**: `src/main/java/com/smartpark/service/AccountVerificationService.java`

**Current** (already correct):
```java
@Async
public void sendVerificationEmailAsync(StaffAccount account, String baseUrl) {
    // Email sending code
}
```

**Verification needed**:
- Check `@EnableAsync` có được enable trong `SmartparkApplication.java` không
- Verify các methods gọi `sendVerificationEmailAsync()` không nằm trong `@Transactional`

**File**: `src/main/java/com/smartpark/SmartparkApplication.java`

**Expected**:
```java
@SpringBootApplication
@EnableScheduling
@EnableAsync  // ← Verify this exists
public class SmartparkApplication {
    // ...
}
```

**Rationale**: 
- Email sending không block DB transaction
- Tránh cạn kiệt connection pool
- Improve performance

---

### 7. Update Test Endpoint Error Handling

**File**: `src/main/java/com/smartpark/controller/DashboardController.java`

**Current** (method `testEmail`):
```java
@GetMapping("/admin/test-email")
public String testEmail(@RequestParam String email, Model model) {
    try {
        emailService.sendAccountVerificationOTP(email, "Test User", "testuser", "123456");
        model.addAttribute("message", "✓ Email sent successfully to: " + email);
    } catch (Exception e) {
        model.addAttribute("error", "✗ Failed to send email: " + e.getMessage());
        model.addAttribute("stackTrace", getStackTrace(e));
    }
    return "test-email-result";
}
```

**Fixed**:
```java
@GetMapping("/admin/test-email")
public String testEmail(@RequestParam String email, Model model) {
    try {
        System.out.println("=== Testing Email Send ===");
        System.out.println("Recipient: " + email);
        
        emailService.sendAccountVerificationOTP(email, "Test User", "testuser", "123456");
        
        model.addAttribute("message", "✓ Email gửi thành công đến: " + email);
        model.addAttribute("success", true);
    } catch (Exception e) {
        System.err.println("✗ Test email failed: " + e.getMessage());
        e.printStackTrace();
        
        model.addAttribute("error", "✗ Lỗi gửi email: " + e.getMessage());
        model.addAttribute("stackTrace", getStackTraceAsString(e));
        model.addAttribute("success", false);
    }
    return "test-email-result";
}

private String getStackTraceAsString(Exception e) {
    java.io.StringWriter sw = new java.io.StringWriter();
    e.printStackTrace(new java.io.PrintWriter(sw));
    return sw.toString();
}
```

**Rationale**: 
- Catch exception từ EmailService (sau khi EmailService throw thay vì nuốt)
- Hiển thị stack trace đầy đủ để debug
- Logging chi tiết hơn

---

## Environment Variables Setup

### Render Dashboard Configuration

User cần update các biến môi trường sau trên Render:

| Variable | Value | Note |
|----------|-------|------|
| `MAIL_HOST` | `smtp.gmail.com` | Gmail SMTP server |
| `MAIL_PORT` | `465` | SSL port (not 587) |
| `MAIL_USERNAME` | `vovietnhat1996@gmail.com` | Gmail address |
| `MAIL_PASSWORD` | `qjzkpjotkanjbjsa` | **NEW** App Password |

**Steps**:
1. Vào Render Dashboard → chọn service `smartparktest`
2. Vào tab **Environment**
3. Update/Add các biến trên
4. Click **Save Changes** → Render sẽ tự động redeploy

**CRITICAL**: Phải dùng App Password MỚI (`qjzkpjotkanjbjsa`) vì password cũ (`gvofejaenxpsylmo`) đã bị Gmail vô hiệu hóa do leak.

---

## Testing Strategy

### 1. Local Testing (Development)

**Setup env vars**:
```bash
export MAIL_USERNAME=vovietnhat1996@gmail.com
export MAIL_PASSWORD=qjzkpjotkanjbjsa
export MAIL_HOST=smtp.gmail.com
export MAIL_PORT=465
```

**Run app**:
```bash
./mvnw spring-boot:run
```

**Check startup logs**:
```
=== Email Configuration Validation ===
✓ MAIL_USERNAME: vov***@gmail.com
✓ MAIL_PASSWORD: qjzk***
✓ MAIL_HOST: smtp.gmail.com
✓ MAIL_PORT: 465
✓ Email configuration looks valid
=====================================
```

**Test email sending**:
```bash
curl "http://localhost:8080/admin/test-email?email=huyhgbv1204@gmail.com"
```

**Expected**: Email được gửi thành công, không có exception

---

### 2. Production Testing (Render)

**After deploying with new env vars**:

1. **Check deployment logs** trên Render:
   - Tìm section "Email Configuration Validation"
   - Verify tất cả values đều có ✓

2. **Test endpoint**:
   ```
   https://smartparktest.onrender.com/admin/test-email?email=huyhgbv1204@gmail.com
   ```

3. **Expected results**:
   - Response: "✓ Email gửi thành công"
   - Email arrives trong vài giây
   - Không có exception trong logs

4. **Test account creation flow**:
   - Admin tạo tài khoản nhân viên mới với email
   - Verify OTP email được gửi
   - Nhập OTP và set password
   - Verify account được kích hoạt

---

### 3. Bug Condition Exploration Test

**Test Case 1: Invalid Credentials**
```java
// Temporarily set wrong password
// Expected: Clear exception message, not timeout
```

**Test Case 2: Missing Env Vars**
```java
// Unset MAIL_USERNAME
// Expected: App fails to start with clear error
```

**Test Case 3: Wrong Port**
```java
// Set MAIL_PORT=587 with SSL enabled
// Expected: Connection error with clear message
```

**Test Case 4: Email in @Transactional**
```java
// Call email service inside @Transactional method
// Expected: Email still sent (via @Async), no DB connection held
```

---

## Rollback Plan

Nếu fix không hoạt động:

1. **Revert code changes**:
   ```bash
   git revert <commit-hash>
   git push
   ```

2. **Restore old env vars** (nếu cần):
   - Trên Render, restore lại giá trị cũ

3. **Alternative solution**: Sử dụng email service khác (SendGrid, AWS SES) thay vì Gmail SMTP

---

## Success Criteria

✅ **Fix thành công khi**:
1. Test endpoint `/admin/test-email` trả về success
2. Email OTP được gửi và nhận trong vài giây
3. Không có `MailConnectException` trong logs
4. Account creation flow hoạt động end-to-end
5. Password reset flow hoạt động end-to-end
6. Startup logs hiển thị email config validation passed

❌ **Fix thất bại nếu**:
1. Vẫn có connection timeout
2. Email không được gửi
3. Exception vẫn bị nuốt im lặng
4. Credentials vẫn bị hardcode

---

## Security Considerations

1. **Credentials không được commit** vào Git
2. **App Password được rotate** định kỳ (mỗi 3-6 tháng)
3. **Env vars được encrypt** trên Render (Render tự động làm)
4. **Logs không in ra password** (chỉ mask: `qjzk***`)
5. **Email validation** để tránh spam/abuse

---

## Performance Impact

- **Positive**: Email sending không block DB transaction (đã có @Async)
- **Neutral**: Exception throwing có overhead nhỏ nhưng acceptable
- **Neutral**: Config validation chỉ chạy 1 lần khi startup

---

## Dependencies

Không cần thêm dependencies mới. Sử dụng:
- `spring-boot-starter-mail` (đã có)
- `jakarta.mail` (đã có)
- `spring-context` (đã có)

---

## Migration Notes

1. **Không có database migration** (chỉ code và config changes)
2. **Không breaking changes** cho API
3. **Backward compatible** với existing email templates
4. **Cần update env vars trên Render** (critical step)

---

## Monitoring & Alerting

Sau khi deploy, monitor:
1. **Render logs**: Tìm "Email Configuration Validation"
2. **Error logs**: Tìm "Failed to send email"
3. **User reports**: Có nhận được OTP email không
4. **Test endpoint**: Chạy định kỳ để verify email service hoạt động

---

## Documentation Updates

Cần update:
1. **README.md**: Thêm section về email configuration
2. **Deployment guide**: Hướng dẫn setup env vars trên Render
3. **Troubleshooting guide**: Các lỗi email thường gặp và cách fix

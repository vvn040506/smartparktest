# Bugfix Requirements Document

## Introduction

Hệ thống SmartPark hiện không thể gửi email (verification OTP, password reset OTP) do lỗi `MailConnectException: Connection timed out` khi kết nối đến `smtp.gmail.com:587`. Chức năng email đã hoạt động bình thường trước đây nhưng ngừng hoạt động sau khi rollback một số commits. Phân tích cho thấy có nhiều nguyên nhân tiềm ẩn:

1. **Gmail App Password bị leak trên GitHub** → Gmail tự động vô hiệu hóa
2. **Biến môi trường production thiếu hoặc không đúng** → SMTP authentication thất bại
3. **Exception bị nuốt im lặng** → Không phát hiện được lỗi thực sự
4. **Port/Protocol configuration không nhất quán** → Connection timeout
5. **Gửi email trong @Transactional** → Giữ DB connection quá lâu, cạn kiệt connection pool

Bug này ảnh hưởng nghiêm trọng đến luồng đăng ký tài khoản nhân viên và reset password.

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN gửi email verification OTP (qua `sendAccountVerificationOTP`) THEN hệ thống throw `MailConnectException: Connection timed out` và không gửi được email

1.2 WHEN gửi email password reset OTP (qua `sendStaffResetOTP`) THEN hệ thống throw `MailConnectException: Connection timed out` và không gửi được email

1.3 WHEN gọi test endpoint `/admin/test-email?email=xxx` THEN hệ thống trả về connection timeout error

1.4 WHEN `MessagingException` xảy ra trong các method gửi email THEN exception bị catch và chỉ in ra `System.err.println`, controller không biết email thất bại và vẫn trả về "thành công"

1.5 WHEN Gmail App Password bị leak trên public repository THEN Gmail tự động vô hiệu hóa App Password, dẫn đến SMTP authentication thất bại

1.6 WHEN biến môi trường `MAIL_USERNAME` hoặc `MAIL_PASSWORD` không được set trên Render THEN hệ thống sử dụng giá trị default rỗng (`""`), dẫn đến SMTP authentication thất bại

1.7 WHEN `application.properties` hardcode credentials (`spring.mail.username=vovietnhat1996@gmail.com`, `spring.mail.password=gvofejaenxpsylmo`) THEN credentials bị expose trên version control và có thể bị leak

1.8 WHEN gửi email trong method có `@Transactional` (như `sendStaffResetLink`) THEN database connection bị giữ trong suốt thời gian SMTP call (có thể chậm hoặc timeout), dẫn đến cạn kiệt connection pool

1.9 WHEN port configuration không nhất quán giữa dev (587 + STARTTLS) và prod (465 + SSL) THEN có thể gây confusion và connection issues nếu biến môi trường không được set đúng

### Expected Behavior (Correct)

2.1 WHEN gửi email verification OTP (qua `sendAccountVerificationOTP`) với credentials và configuration hợp lệ THEN hệ thống SHALL gửi email thành công đến recipient trong vòng vài giây

2.2 WHEN gửi email password reset OTP (qua `sendStaffResetOTP`) với credentials và configuration hợp lệ THEN hệ thống SHALL gửi email thành công đến recipient trong vòng vài giây

2.3 WHEN gọi test endpoint `/admin/test-email?email=xxx` với credentials và configuration hợp lệ THEN hệ thống SHALL trả về thông báo thành công và email được gửi đến địa chỉ test

2.4 WHEN `MessagingException` xảy ra trong các method gửi email THEN hệ thống SHALL throw exception lên caller để controller có thể xử lý và thông báo lỗi cho user

2.5 WHEN sử dụng Gmail SMTP THEN hệ thống SHALL sử dụng App Password hợp lệ (chưa bị vô hiệu hóa) và không bị leak trên public repository

2.6 WHEN deploy lên production (Render) THEN hệ thống SHALL đọc credentials từ biến môi trường `MAIL_USERNAME` và `MAIL_PASSWORD` (không có default rỗng)

2.7 WHEN lưu trữ credentials THEN hệ thống SHALL sử dụng biến môi trường thay vì hardcode trong `application.properties`, và SHALL sử dụng placeholder `${MAIL_USERNAME}` và `${MAIL_PASSWORD}` không có default value

2.8 WHEN gửi email THEN hệ thống SHALL thực hiện SMTP call bên ngoài `@Transactional` context (sử dụng `@Async` hoặc tách riêng transaction) để tránh giữ database connection quá lâu

2.9 WHEN cấu hình SMTP cho Gmail THEN hệ thống SHALL sử dụng configuration nhất quán: port 465 với SSL enabled (`spring.mail.properties.mail.smtp.ssl.enable=true`) và STARTTLS disabled (`spring.mail.properties.mail.smtp.starttls.enable=false`)

### Unchanged Behavior (Regression Prevention)

3.1 WHEN email được gửi thành công THEN nội dung email (HTML template, OTP code, links) SHALL CONTINUE TO được format và hiển thị đúng như hiện tại

3.2 WHEN gửi email verification OTP THEN OTP code SHALL CONTINUE TO là 6 chữ số ngẫu nhiên và hết hạn sau 10 phút

3.3 WHEN gửi email password reset OTP THEN OTP code SHALL CONTINUE TO là 6 chữ số ngẫu nhiên và hết hạn sau 10 phút

3.4 WHEN gửi email với link (verification link, reset link) THEN link format và expiry time SHALL CONTINUE TO hoạt động như hiện tại

3.5 WHEN `sendVerificationEmailAsync` được gọi THEN method SHALL CONTINUE TO chạy bất đồng bộ với `@Async` annotation

3.6 WHEN email service được inject vào các controllers và services THEN dependency injection SHALL CONTINUE TO hoạt động bình thường

3.7 WHEN sử dụng `MimeMessage` và `MimeMessageHelper` THEN email encoding (UTF-8) và HTML rendering SHALL CONTINUE TO hoạt động đúng

3.8 WHEN scheduled job `cleanupExpiredTokens` chạy THEN việc cleanup tokens và accounts SHALL CONTINUE TO hoạt động bình thường và không bị ảnh hưởng bởi email configuration changes

package com.smartpark.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Value("${resend.api.key:}")
    private String resendApiKey;

    @Value("${app.email.from:onboarding@resend.dev}")
    private String fromEmail;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private void sendEmail(String to, String subject, String html) {
        // Ưu tiên sử dụng Resend nếu có API Key
        if (resendApiKey != null && !resendApiKey.isEmpty() && !resendApiKey.startsWith("${")) {
            try {
                System.out.println("🚀 [RESEND] Đang gửi email tới: " + to);
                Resend resend = new Resend(resendApiKey);
                CreateEmailOptions options = CreateEmailOptions.builder()
                        .from(fromEmail)
                        .to(to)
                        .subject(subject)
                        .html(html)
                        .build();
                CreateEmailResponse response = resend.emails().send(options);
                System.out.println("✅ [RESEND] Gửi thành công tới: " + to + " (ID: " + response.getId() + ")");
                return;
            } catch (ResendException e) {
                System.err.println("❌ [RESEND LỖI] " + e.getMessage());
            }
        }

        // Nếu không có Resend, thử dùng JavaMailSender (Gmail)
        if (mailSender != null) {
            try {
                System.out.println("🚀 [GMAIL] Đang gửi email tới: " + to);
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromEmail);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(html, true);
                mailSender.send(message);
                System.out.println("✅ [GMAIL] Gửi thành công tới: " + to);
            } catch (MessagingException e) {
                System.err.println("❌ [GMAIL LỖI] " + e.getMessage());
            }
        } else {
            System.err.println("❌ [LỖI] Chưa cấu hình cả Resend và Gmail!");
        }
    }

    public void sendResetEmail(String toEmail, String resetLink) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;
                            border:1px solid #ddd;border-radius:8px;padding:24px">
                  <h2 style="color:#1a73e8">🅿️ SmartPark</h2>
                  <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
                  <p>Nhấn nút bên dưới để đặt lại mật khẩu (link hết hạn sau <b>30 phút</b>):</p>
                  <a href="%s"
                     style="display:inline-block;margin:16px 0;padding:12px 24px;
                            background:#1a73e8;color:#fff;border-radius:6px;
                            text-decoration:none;font-weight:bold">
                    Đặt lại mật khẩu
                  </a>
                  <p style="color:#888;font-size:12px">
                    Nếu bạn không yêu cầu, hãy bỏ qua email này.
                  </p>
                </div>
            """.formatted(resetLink);
        sendEmail(toEmail, "🅿️ SmartPark – Đặt lại mật khẩu", html);
    }

    public void sendAccountVerificationEmail(String toEmail, String verifyLink, String username) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;
                            border:1px solid #ddd;border-radius:8px;padding:24px">
                  <h2 style="color:#6c5ce7">🅿️ SmartPark</h2>
                  <p>Chào mừng bạn đến với hệ thống quản lý bãi đỗ xe SmartPark!</p>
                  <p>Admin đã tạo tài khoản nhân viên cho bạn. Vui lòng xác nhận email và đặt mật khẩu để kích hoạt tài khoản.</p>
                  
                  <div style="background:#f0f4f8;border-radius:8px;padding:16px;margin:16px 0">
                    <p style="margin:4px 0"><strong>Tên đăng nhập:</strong> <code style="background:#fff;padding:4px 8px;border-radius:4px">%s</code></p>
                  </div>
                  
                  <p>Nhấn nút bên dưới để xác nhận email và đặt mật khẩu (link hết hạn sau <b>5 phút</b>):</p>
                  <a href="%s"
                     style="display:inline-block;margin:16px 0;padding:12px 24px;
                            background:#6c5ce7;color:#fff;border-radius:6px;
                            text-decoration:none;font-weight:bold">
                    ✓ Xác nhận và đặt mật khẩu
                  </a>
                  <p style="color:#888;font-size:12px">
                    Sau khi đặt mật khẩu, bạn có thể đăng nhập ngay.
                  </p>
                  <p style="color:#888;font-size:12px">
                    Nếu bạn không yêu cầu tài khoản này, hãy bỏ qua email này.
                  </p>
                </div>
            """.formatted(username, verifyLink);
        sendEmail(toEmail, "🅿️ SmartPark – Xác nhận tài khoản nhân viên", html);
    }

    public void sendStaffResetEmail(String toEmail, String fullName, String resetLink) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;
                            border:1px solid #ddd;border-radius:8px;padding:24px">
                  <h2 style="color:#1a73e8">🅿️ SmartPark</h2>
                  <p>Xin chào <strong>%s</strong>,</p>
                  <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản nhân viên của bạn.</p>
                  <p>Nhấn nút bên dưới để đặt lại mật khẩu (link hết hạn sau <b>30 phút</b>):</p>
                  <a href="%s"
                     style="display:inline-block;margin:16px 0;padding:12px 24px;
                            background:#1a73e8;color:#fff;border-radius:6px;
                            text-decoration:none;font-weight:bold">
                    Đặt lại mật khẩu
                  </a>
                  <p style="color:#888;font-size:12px">
                    Nếu bạn không yêu cầu, hãy bỏ qua email này.
                  </p>
                </div>
            """.formatted(fullName, resetLink);
        sendEmail(toEmail, "🅿️ SmartPark – Đặt lại mật khẩu nhân viên", html);
    }

    public void sendStaffResetOTP(String toEmail, String fullName, String otpCode) {
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;
                            border:1px solid #ddd;border-radius:8px;padding:24px">
                  <h2 style="color:#1a73e8">🅿️ SmartPark</h2>
                  <p>Xin chào <strong>%s</strong>,</p>
                  <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản nhân viên của bạn.</p>
                  
                  <div style="background:#f0f4f8;border-radius:8px;padding:24px;margin:24px 0;text-align:center">
                    <p style="margin:0;color:#666;font-size:14px">Mã OTP của bạn:</p>
                    <p style="margin:8px 0;font-size:32px;font-weight:bold;color:#1a73e8;letter-spacing:8px">%s</p>
                    <p style="margin:0;color:#888;font-size:12px">Mã hết hạn sau <b>10 phút</b></p>
                  </div>
                  
                  <p style="color:#888;font-size:12px">
                    Nếu bạn không yêu cầu, hãy bỏ qua email này.
                  </p>
                  <p style="color:#888;font-size:12px">
                    <b>Lưu ý:</b> Không chia sẻ mã OTP này với bất kỳ ai.
                  </p>
                </div>
            """.formatted(fullName, otpCode);
        sendEmail(toEmail, "🅿️ SmartPark – Mã OTP đặt lại mật khẩu", html);
    }

    public void sendAccountVerificationOTP(String toEmail, String fullName, String username, String otpCode) {
        // Log OTP ra console để test local nhanh
        System.out.println("\n**************************************************");
        System.out.println("📢 MÃ OTP CỦA BẠN: " + otpCode);
        System.out.println("Gửi đến: " + toEmail);
        System.out.println("**************************************************\n");

        String html = """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;
                            border:1px solid #ddd;border-radius:8px;padding:24px">
                  <h2 style="color:#6c5ce7">🅿️ SmartPark</h2>
                  <p>Chào mừng <strong>%s</strong> đến với SmartPark!</p>
                  <p>Vui lòng sử dụng mã OTP bên dưới để kích hoạt tài khoản của bạn.</p>
                  
                  <div style="background:#f0f4f8;border-radius:8px;padding:24px;margin:24px 0;text-align:center">
                    <p style="margin:0;color:#666;font-size:14px">Mã OTP xác nhận của bạn:</p>
                    <p style="margin:8px 0;font-size:32px;font-weight:bold;color:#6c5ce7;letter-spacing:8px">%s</p>
                    <p style="margin:0;color:#888;font-size:12px">Mã hết hạn sau <b>10 phút</b></p>
                  </div>
                  
                  <p style="color:#888;font-size:12px">
                    Nếu bạn không thực hiện đăng ký, hãy bỏ qua email này.
                  </p>
                </div>
            """.formatted(fullName, otpCode);
        sendEmail(toEmail, "🅿️ SmartPark – Mã OTP kích hoạt tài khoản", html);
    }

    // Fix #7: Gửi email thông báo kích hoạt thẻ tháng thành công
    public void sendMonthlyPassActivatedEmail(String toEmail, String ownerName,
                                               String licensePlate, String paymentCode,
                                               LocalDate startDate, LocalDate endDate,
                                               long amountPaid) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String html = """
            <div style="font-family: sans-serif; max-width: 600px; margin: auto; border: 1px solid #eee; padding: 20px; border-radius: 10px;">
                <h2 style="color: #00875a; text-align: center;">✅ Thẻ tháng đã kích hoạt!</h2>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Thẻ tháng SmartPark của bạn đã được kích hoạt thành công sau khi nhận được thanh toán.</p>
                <div style="background: #f9f9f9; padding: 15px; border-radius: 8px; margin: 20px 0;">
                    <p style="margin: 5px 0;">🚗 Biển số: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">📅 Hiệu lực: <strong>%s</strong> đến <strong>%s</strong></p>
                    <p style="margin: 5px 0;">💰 Số tiền: <strong>%,d đ</strong></p>
                    <p style="margin: 5px 0;">🔑 Mã thẻ: <strong>%s</strong></p>
                </div>
                <p>Bạn có thể sử dụng mã thẻ hoặc QR code trong ứng dụng để ra vào bãi xe tự do trong thời gian hiệu lực.</p>
                <p style="color: #666; font-size: 12px; text-align: center; margin-top: 30px;">
                    Đây là email tự động, vui lòng không phản hồi.<br>
                    © 2024 SmartPark System
                </p>
            </div>
            """.formatted(ownerName, licensePlate,
                startDate.format(fmt), endDate.format(fmt), amountPaid, paymentCode);

        sendEmail(toEmail, "🅿️ SmartPark – Thẻ tháng của bạn đã được kích hoạt", html);
    }

    public void sendBookingPaidEmail(String toEmail, String customerName,
                                     String licensePlate, String paymentCode,
                                     String slotId, long amountPaid) {
        String html = """
            <div style="font-family: sans-serif; max-width: 600px; margin: auto; border: 1px solid #eee; padding: 20px; border-radius: 10px;">
                <h2 style="color: #1a73e8; text-align: center;">✅ Thanh toán vé đặt trước thành công!</h2>
                <p>Xin chào <strong>%s</strong>,</p>
                <p>Cảm ơn bạn đã sử dụng dịch vụ đặt chỗ trước của SmartPark. Thanh toán của bạn đã được ghi nhận.</p>
                <div style="background: #f9f9f9; padding: 15px; border-radius: 8px; margin: 20px 0;">
                    <p style="margin: 5px 0;">🚗 Biển số: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">📍 Vị trí ô đỗ: <strong>%s</strong></p>
                    <p style="margin: 5px 0;">💰 Số tiền: <strong>%,d đ</strong></p>
                    <p style="margin: 5px 0;">🔑 Mã vé: <strong>%s</strong></p>
                </div>
                <p>Vui lòng xuất trình mã vé hoặc QR code khi đến bãi xe để nhân viên xác nhận cho xe vào.</p>
                <p style="color: #666; font-size: 12px; text-align: center; margin-top: 30px;">
                    Đây là email tự động, vui lòng không phản hồi.<br>
                    © 2024 SmartPark System
                </p>
            </div>
            """.formatted(customerName, licensePlate, slotId, amountPaid, paymentCode);

        sendEmail(toEmail, "🅿️ SmartPark – Xác nhận thanh toán vé đặt trước", html);
    }
}
# Implementation Plan

- [x] 1. Write bug condition exploration test
  - **Property 1: Bug Condition** - Email Sending Failures với Invalid/Missing Credentials
  - **CRITICAL**: Test này PHẢI FAIL trên unfixed code - failure xác nhận bug tồn tại
  - **DO NOT attempt to fix the test or the code when it fails**
  - **NOTE**: Test này encode expected behavior - sẽ validate fix khi pass sau implementation
  - **GOAL**: Surface counterexamples chứng minh bug tồn tại
  - **Scoped PBT Approach**: Scope property đến các concrete failing cases để đảm bảo reproducibility
  - Tạo file test: `src/test/java/com/smartpark/bugfix/EmailSendingBugConditionTest.java`
  - Test các bug conditions từ design:
    - **BC1**: `isBugCondition(config) = (config.username == "" OR config.password == "")`
      - Test: Khi MAIL_USERNAME hoặc MAIL_PASSWORD rỗng → email sending SHOULD fail với clear exception
      - Counterexample: `sendEmail(toEmail="test@example.com", username="", password="valid")` → timeout/auth failure
    - **BC2**: `isBugCondition(config) = (config.password == "gvofejaenxpsylmo")` (leaked password)
      - Test: Khi sử dụng leaked App Password → email sending SHOULD fail với authentication error
      - Counterexample: `sendEmail(toEmail="test@example.com", password="gvofejaenxpsylmo")` → auth failure
    - **BC3**: `isBugCondition(emailService) = (emailService catches MessagingException AND only logs)`
      - Test: Khi MessagingException xảy ra → exception SHOULD be thrown to caller (not swallowed)
      - Counterexample: `sendEmail(invalidConfig)` → no exception thrown, only stderr log
    - **BC4**: `isBugCondition(config) = (config.port == 587 AND config.ssl.enable == true)` (port mismatch)
      - Test: Khi port 587 với SSL enabled → connection SHOULD fail với clear error
      - Counterexample: `sendEmail(port=587, ssl=true)` → connection timeout
  - Test assertions match Expected Behavior Properties từ design:
    - **P1**: `expectedBehavior(result) = (result.success == true AND result.emailSent == true)`
    - **P2**: `expectedBehavior(exception) = (exception IS MessagingException OR RuntimeException)`
    - **P3**: `expectedBehavior(config) = (config.username != "" AND config.password != "")`
  - Run test trên UNFIXED code
  - **EXPECTED OUTCOME**: Test FAILS (đúng - chứng minh bug tồn tại)
  - Document counterexamples tìm được để hiểu root cause
  - Mark task complete khi test được viết, chạy, và failure được document
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7_

- [x] 2. Write preservation property tests (BEFORE implementing fix)
  - **Property 2: Preservation** - Email Content và Async Behavior
  - **IMPORTANT**: Follow observation-first methodology
  - Observe behavior trên UNFIXED code cho non-buggy inputs
  - Write property-based tests capturing observed behavior patterns từ Preservation Requirements
  - Property-based testing generates nhiều test cases cho stronger guarantees
  - Tạo file test: `src/test/java/com/smartpark/bugfix/EmailSendingPreservationTest.java`
  - Test các preservation requirements từ design:
    - **PR1**: Email content (HTML template, OTP code, links) format đúng
      - Observe: `sendAccountVerificationOTP(email, fullName, username, "123456")` → email body contains OTP "123456"
      - Property: FOR ALL valid inputs, email body MUST contain correct OTP code và HTML formatting
    - **PR2**: OTP code là 6 chữ số và hết hạn sau 10 phút
      - Observe: Generated OTP codes are 6 digits
      - Property: FOR ALL OTP generation calls, code length == 6 AND expiry == 10 minutes
    - **PR3**: `@Async` annotation hoạt động bình thường
      - Observe: `sendVerificationEmailAsync()` returns immediately without blocking
      - Property: FOR ALL async email calls, method returns before email is actually sent
    - **PR4**: Dependency injection hoạt động
      - Observe: EmailService được inject vào controllers/services
      - Property: FOR ALL beans using EmailService, injection succeeds
    - **PR5**: UTF-8 encoding và HTML rendering
      - Observe: Vietnamese characters render correctly in email
      - Property: FOR ALL emails with Vietnamese text, encoding == UTF-8 AND rendering correct
  - Run tests trên UNFIXED code
  - **EXPECTED OUTCOME**: Tests PASS (xác nhận baseline behavior để preserve)
  - Mark task complete khi tests được viết, chạy, và passing trên unfixed code
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8_

- [x] 3. Fix for Email Sending Connection Timeout

  - [x] 3.1 Remove hardcoded credentials từ application.properties
    - Mở file `src/main/resources/application.properties`
    - Thay thế hardcoded values:
      ```properties
      spring.mail.username=vovietnhat1996@gmail.com
      spring.mail.password=gvofejaenxpsylmo
      ```
    - Bằng environment variable placeholders:
      ```properties
      spring.mail.username=${MAIL_USERNAME}
      spring.mail.password=${MAIL_PASSWORD}
      ```
    - Standardize SMTP configuration cho Gmail (port 465 + SSL):
      ```properties
      spring.mail.port=${MAIL_PORT:465}
      spring.mail.properties.mail.smtp.starttls.enable=false
      spring.mail.properties.mail.smtp.ssl.enable=true
      spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
      ```
    - _Bug_Condition: isBugCondition(config) = (config.username hardcoded OR config.password hardcoded)_
    - _Expected_Behavior: config.username = ${MAIL_USERNAME} AND config.password = ${MAIL_PASSWORD}_
    - _Preservation: Email content formatting và async behavior không thay đổi_
    - _Requirements: 1.7, 2.7, 2.9_

  - [x] 3.2 Remove default empty values từ application-prod.properties
    - Mở file `src/main/resources/application-prod.properties`
    - Thay thế:
      ```properties
      spring.mail.username=${MAIL_USERNAME:}
      spring.mail.password=${MAIL_PASSWORD:}
      ```
    - Bằng (không có default):
      ```properties
      spring.mail.username=${MAIL_USERNAME}
      spring.mail.password=${MAIL_PASSWORD}
      ```
    - Verify port configuration đã đúng (465 + SSL):
      ```properties
      spring.mail.port=${MAIL_PORT:465}
      spring.mail.properties.mail.smtp.ssl.enable=true
      spring.mail.properties.mail.smtp.starttls.enable=false
      ```
    - _Bug_Condition: isBugCondition(config) = (config.username == "" OR config.password == "")_
    - _Expected_Behavior: App fails to start nếu MAIL_USERNAME hoặc MAIL_PASSWORD không được set_
    - _Preservation: Existing prod configuration cho database và other services không thay đổi_
    - _Requirements: 1.6, 2.6, 2.9_

  - [x] 3.3 Throw exceptions thay vì swallow trong EmailService
    - Mở file `src/main/java/com/smartpark/service/EmailService.java`
    - Update tất cả 5 methods:
      - `sendResetEmail()`
      - `sendAccountVerificationEmail()`
      - `sendStaffResetEmail()`
      - `sendStaffResetOTP()`
      - `sendAccountVerificationOTP()`
    - Thay thế pattern:
      ```java
      } catch (MessagingException e) {
          System.err.println("Lỗi gửi email: " + e.getMessage());
      }
      ```
    - Bằng:
      ```java
      } catch (MessagingException e) {
          System.err.println("✗ Failed to send email to " + toEmail + ": " + e.getMessage());
          throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
      }
      ```
    - Thêm success logging:
      ```java
      mailSender.send(msg);
      System.out.println("✓ Email sent successfully to: " + toEmail);
      ```
    - _Bug_Condition: isBugCondition(emailService) = (emailService catches MessagingException AND only logs)_
    - _Expected_Behavior: MessagingException được throw lên caller với full stack trace_
    - _Preservation: Email content và templates không thay đổi_
    - _Requirements: 1.4, 2.4_

  - [x] 3.4 Create EmailConfigValidator component
    - Tạo file mới: `src/main/java/com/smartpark/config/EmailConfigValidator.java`
    - Implement validation logic:
      - Inject `@Value` cho mail.username, mail.password, mail.host, mail.port
      - Listen to `ApplicationReadyEvent`
      - Validate credentials không null/empty
      - Log configuration status với masking (email: `vov***@gmail.com`, password: `qjzk***`)
      - Warn nếu credentials thiếu
    - Code template từ design document section 5
    - _Bug_Condition: isBugCondition(startup) = (no validation of email config)_
    - _Expected_Behavior: Clear warning logs khi email config invalid_
    - _Preservation: App startup flow không bị ảnh hưởng_
    - _Requirements: 2.6, 2.7_

  - [x] 3.5 Verify @Async configuration
    - Mở file `src/main/java/com/smartpark/SmartparkApplication.java`
    - Verify `@EnableAsync` annotation tồn tại:
      ```java
      @SpringBootApplication
      @EnableScheduling
      @EnableAsync  // ← Must exist
      public class SmartparkApplication {
      ```
    - Nếu thiếu, thêm `@EnableAsync`
    - Verify `AccountVerificationService.sendVerificationEmailAsync()` có `@Async`
    - _Bug_Condition: isBugCondition(emailCall) = (emailCall inside @Transactional AND not @Async)_
    - _Expected_Behavior: Email sending không block DB transaction_
    - _Preservation: Existing async behavior được maintain_
    - _Requirements: 1.8, 2.8, 3.5_

  - [x] 3.6 Update test endpoint error handling
    - Mở file `src/main/java/com/smartpark/controller/DashboardController.java`
    - Tìm method `testEmail()`
    - Update error handling để catch exception từ EmailService
    - Thêm logging chi tiết:
      ```java
      System.out.println("=== Testing Email Send ===");
      System.out.println("Recipient: " + email);
      ```
    - Thêm stack trace display trong model:
      ```java
      model.addAttribute("stackTrace", getStackTraceAsString(e));
      ```
    - Implement helper method `getStackTraceAsString()`
    - _Bug_Condition: isBugCondition(testEndpoint) = (endpoint không catch exception từ EmailService)_
    - _Expected_Behavior: Test endpoint hiển thị full error details khi email fail_
    - _Preservation: Test endpoint functionality không thay đổi khi email success_
    - _Requirements: 1.3, 2.3_

  - [x] 3.7 Verify bug condition exploration test now passes
    - **Property 1: Expected Behavior** - Email Sending Success với Valid Credentials
    - **IMPORTANT**: Re-run SAME test từ task 1 - KHÔNG viết test mới
    - Test từ task 1 encode expected behavior
    - Khi test này pass, xác nhận expected behavior được satisfy
    - Run bug condition exploration test từ step 1
    - **EXPECTED OUTCOME**: Test PASSES (xác nhận bug đã fix)
    - Verify tất cả bug conditions đã được resolve:
      - BC1: Empty credentials → app fails to start (not silent failure)
      - BC2: Leaked password → không còn hardcoded
      - BC3: Exception swallowing → exceptions được thrown
      - BC4: Port mismatch → standardized to 465 + SSL
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9_

  - [x] 3.8 Verify preservation tests still pass
    - **Property 2: Preservation** - Email Content và Async Behavior Unchanged
    - **IMPORTANT**: Re-run SAME tests từ task 2 - KHÔNG viết tests mới
    - Run preservation property tests từ step 2
    - **EXPECTED OUTCOME**: Tests PASS (xác nhận không có regressions)
    - Verify tất cả preservation requirements:
      - PR1: Email content formatting đúng
      - PR2: OTP code 6 digits, expiry 10 minutes
      - PR3: @Async hoạt động
      - PR4: Dependency injection hoạt động
      - PR5: UTF-8 encoding đúng
    - Confirm all tests still pass sau fix (no regressions)

- [x] 4. Environment Setup và Production Testing

  - [x] 4.1 Setup local environment variables
    - Tạo file `.env.local` (KHÔNG commit vào Git):
      ```bash
      export MAIL_USERNAME=vovietnhat1996@gmail.com
      export MAIL_PASSWORD=qjzkpjotkanjbjsa
      export MAIL_HOST=smtp.gmail.com
      export MAIL_PORT=465
      ```
    - Source file: `source .env.local`
    - Run app: `./mvnw spring-boot:run`
    - Verify startup logs hiển thị email config validation passed

  - [x] 4.2 Update Render environment variables
    - Document steps cho user:
      1. Vào Render Dashboard → chọn service `smartparktest`
      2. Vào tab **Environment**
      3. Update/Add các biến:
         - `MAIL_HOST=smtp.gmail.com`
         - `MAIL_PORT=465`
         - `MAIL_USERNAME=vovietnhat1996@gmail.com`
         - `MAIL_PASSWORD=qjzkpjotkanjbjsa` (NEW App Password)
      4. Click **Save Changes** → Render auto redeploy
    - **CRITICAL**: Phải dùng NEW App Password vì old password đã bị Gmail disable do leak

  - [x] 4.3 Test local email sending
    - Run test endpoint: `curl "http://localhost:8080/admin/test-email?email=huyhgbv1204@gmail.com"`
    - Verify response: "✓ Email gửi thành công"
    - Check email inbox: OTP email arrives trong vài giây
    - Verify logs: Không có exception, có "✓ Email sent successfully"

  - [x] 4.4 Test production email sending
    - After Render redeploy, check deployment logs
    - Tìm section "Email Configuration Validation"
    - Verify tất cả values có ✓
    - Test endpoint: `https://smartparktest.onrender.com/admin/test-email?email=huyhgbv1204@gmail.com`
    - Verify email arrives
    - Test account creation flow end-to-end
    - Test password reset flow end-to-end

- [x] 5. Checkpoint - Ensure all tests pass
  - Run full test suite: `./mvnw test`
  - Verify bug condition test passes (confirms fix works)
  - Verify preservation tests pass (confirms no regressions)
  - Verify manual testing passed:
    - Local email sending works
    - Production email sending works
    - Account creation flow works
    - Password reset flow works
  - Check logs: Không có exceptions, email config validation passed
  - Nếu có vấn đề, ask user trước khi proceed

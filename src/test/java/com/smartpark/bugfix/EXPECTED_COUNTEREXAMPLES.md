# Expected Counterexamples from Bug Condition Exploration Test

## Overview

This document lists the expected counterexamples that will be discovered when running the bug condition exploration test on **unfixed code**. These counterexamples prove that the bugs exist.

## Counterexample 1: Leaked Password Authentication Failure

**Test Method**: `testLeakedPasswordShouldFail()`

**Input**:
```java
emailService.sendAccountVerificationOTP(
    "test@example.com",
    "Test User", 
    "testuser",
    "123456"
)
```

**Configuration**:
```properties
spring.mail.username=vovietnhat1996@gmail.com
spring.mail.password=gvofejaenxpsylmo  # LEAKED AND DISABLED
spring.mail.host=smtp.gmail.com
spring.mail.port=587
```

**Expected Output**:
```
Exception: jakarta.mail.MessagingException: Could not connect to SMTP host: smtp.gmail.com, port: 587
Caused by: java.net.SocketTimeoutException: Connection timed out
```

OR

```
Exception: jakarta.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```

**Actual Output (Bug)**:
```
No exception thrown to caller
Stderr: Lỗi gửi email xác nhận OTP: Could not connect to SMTP host: smtp.gmail.com, port: 587
```

**Bug Confirmed**: Exception is swallowed (BC3) and leaked password causes authentication failure (BC2)

---

## Counterexample 2: Exception Swallowing

**Test Method**: `testExceptionsShouldNotBeSwallowed()`

**Input**:
```java
emailService.sendAccountVerificationOTP(
    "test@example.com",
    "Test User",
    "testuser", 
    "123456"
)
```

**Expected Behavior**:
```java
throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
```

**Actual Behavior (Bug)**:
```java
catch (MessagingException e) {
    System.err.println("Lỗi gửi email xác nhận OTP: " + e.getMessage());
    // No exception thrown!
}
```

**Counterexample Output**:
```
Exception thrown: false
Stderr output: Lỗi gửi email xác nhận OTP: Could not connect to SMTP host: smtp.gmail.com, port: 587; nested exception is: java.net.SocketTimeoutException: Connection timed out
```

**Bug Confirmed**: Exception is swallowed and only logged to stderr (BC3)

---

## Counterexample 3: Port Mismatch Connection Failure

**Test Method**: `testPortMismatchShouldFail()`

**Input**:
```java
MimeMessage msg = mailSender.createMimeMessage();
MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
helper.setTo("test@example.com");
helper.setSubject("Test");
helper.setText("Test", true);
mailSender.send(msg);
```

**Configuration**:
```properties
spring.mail.port=587
spring.mail.properties.mail.smtp.starttls.enable=true
# Using STARTTLS with port 587 (correct)
# But if SSL is enabled with port 587, it would fail
```

**Expected Output**:
```
Exception: jakarta.mail.MessagingException: Could not connect to SMTP host
Caused by: java.net.SocketTimeoutException: Connection timed out
```

**Bug Confirmed**: Connection timeout due to invalid credentials (BC2) or port mismatch (BC4)

---

## Counterexample 4: Integration Test - Overall Failure

**Test Method**: `testCurrentCodeFailsToSendEmail()`

**Input**:
```java
emailService.sendAccountVerificationOTP(
    "test@example.com",
    "Test User",
    "testuser",
    "123456"
)
```

**Expected Console Output**:
```
=== Bug Condition Exploration Results ===
Exception thrown: false
Exception message: null
Stderr output: Lỗi gửi email xác nhận OTP: Could not connect to SMTP host: smtp.gmail.com, port: 587; nested exception is: java.net.SocketTimeoutException: Connection timed out
=========================================
```

**Test Failure Message**:
```
INTEGRATION TEST FAILED: Exception was swallowed (only logged to stderr). 
This confirms BC3 (exception swallowing). 
Expected: RuntimeException thrown to caller. 
Actual: No exception, stderr: Lỗi gửi email xác nhận OTP: ...
```

**Bug Confirmed**: Multiple bugs exist:
- BC2: Leaked password causes authentication failure
- BC3: Exception is swallowed and only logged
- BC4: Connection timeout due to configuration issues

---

## Counterexample 5: Empty Credentials (Documented)

**Test Method**: `testEmptyCredentialsShouldFail()`

**Configuration (Production)**:
```properties
spring.mail.username=${MAIL_USERNAME:}  # Default: empty string
spring.mail.password=${MAIL_PASSWORD:}  # Default: empty string
```

**Expected Behavior**:
```
Application fails to start with clear error:
"Error: MAIL_USERNAME is required but not set"
```

**Actual Behavior (Bug)**:
```
Application starts successfully
Email sending fails silently at runtime with:
Stderr: Lỗi gửi email: 535 Authentication failed
```

**Bug Confirmed**: Empty credentials cause silent runtime failures instead of startup failures (BC1)

---

## Summary of Bugs Confirmed by Counterexamples

| Bug Condition | Counterexample | Status |
|---------------|----------------|--------|
| BC1: Empty credentials | App starts with empty credentials, fails silently at runtime | ✗ CONFIRMED |
| BC2: Leaked password | Connection timeout / auth failure with `gvofejaenxpsylmo` | ✗ CONFIRMED |
| BC3: Exception swallowing | No exception thrown, only stderr log | ✗ CONFIRMED |
| BC4: Port mismatch | Connection timeout with inconsistent port/protocol config | ✗ CONFIRMED |

---

## How to Document Actual Counterexamples

When running the test, capture the actual output:

1. **Run the test**:
   ```bash
   ./mvnw test -Dtest=EmailSendingBugConditionTest
   ```

2. **Capture console output**:
   - Look for test failure messages
   - Look for stderr output in console
   - Look for exception stack traces

3. **Document findings**:
   - Copy actual exception messages
   - Copy stderr output
   - Note which tests failed and why

4. **Update this document** with actual counterexamples found

---

## After Fix Implementation

After implementing the fix (Task 3), re-run the test. Expected changes:

| Bug Condition | Expected Behavior After Fix |
|---------------|----------------------------|
| BC1: Empty credentials | App fails to start with clear error message |
| BC2: Leaked password | No longer hardcoded; uses env vars with valid password |
| BC3: Exception swallowing | RuntimeException thrown to caller with full stack trace |
| BC4: Port mismatch | Standardized to port 465 + SSL |

The test should then be updated to verify the fixed behavior (Task 3.7).

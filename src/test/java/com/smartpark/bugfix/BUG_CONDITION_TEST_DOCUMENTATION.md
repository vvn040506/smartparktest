# Bug Condition Exploration Test Documentation

## Overview

This document describes the bug condition exploration test for the email sending connection timeout fix. The test is designed to **FAIL on unfixed code** to confirm that the bugs exist.

## Test File

`src/test/java/com/smartpark/bugfix/EmailSendingBugConditionTest.java`

## Purpose

The test validates 4 bug conditions identified in the bugfix design:

### BC1: Empty Credentials
- **Bug Condition**: `isBugCondition(config) = (config.username == "" OR config.password == "")`
- **Expected Behavior**: Email sending SHOULD fail with clear exception (not silent failure)
- **Counterexample**: `sendEmail(username="", password="valid")` → timeout/auth failure
- **Current Issue**: `application-prod.properties` has default empty values: `${MAIL_USERNAME:}` and `${MAIL_PASSWORD:}`
- **Expected Fix**: Remove default empty values so app fails fast on startup

### BC2: Leaked Password
- **Bug Condition**: `isBugCondition(config) = (config.password == "gvofejaenxpsylmo")`
- **Expected Behavior**: Using leaked App Password SHOULD fail with authentication error
- **Counterexample**: `sendEmail(password="gvofejaenxpsylmo")` → auth failure
- **Current Issue**: Password `gvofejaenxpsylmo` was leaked on GitHub and disabled by Gmail
- **Expected Fix**: Use environment variables, not hardcoded values

### BC3: Exception Swallowing
- **Bug Condition**: `isBugCondition(emailService) = (emailService catches MessagingException AND only logs)`
- **Expected Behavior**: MessagingException SHOULD be thrown to caller (not swallowed)
- **Counterexample**: `sendEmail(invalidConfig)` → no exception thrown, only stderr log
- **Current Issue**: All methods in `EmailService.java` catch `MessagingException` and only log to stderr
- **Expected Fix**: Throw `RuntimeException` wrapping `MessagingException`

### BC4: Port Mismatch
- **Bug Condition**: `isBugCondition(config) = (config.port == 587 AND config.ssl.enable == true)`
- **Expected Behavior**: Port 587 with SSL enabled SHOULD fail with clear connection error
- **Counterexample**: `sendEmail(port=587, ssl=true)` → connection timeout
- **Current Issue**: Inconsistent port configuration between dev (587 + STARTTLS) and prod (465 + SSL)
- **Expected Fix**: Standardize to port 465 + SSL for Gmail

## Test Methods

### 1. `testEmptyCredentialsShouldFail()`
Documents that empty credentials should cause startup failure, not silent runtime failure.

### 2. `testLeakedPasswordShouldFail()`
Attempts to send email with leaked credentials. **Expected to fail** with authentication error.

**Expected Counterexample**:
```
Exception: MailConnectException or AuthenticationFailedException
Message: Connection timed out OR Authentication failed
```

### 3. `testExceptionsShouldNotBeSwallowed()`
Verifies that exceptions are thrown to caller, not swallowed and only logged.

**Expected Counterexample**:
```
No exception thrown
Stderr output: "Lỗi gửi email xác nhận OTP: ..."
```

This confirms BC3: Exception was swallowed.

### 4. `testPortMismatchShouldFail()`
Verifies that connection fails with current port configuration.

**Expected Counterexample**:
```
Exception: MailConnectException
Message: Connection timed out OR Could not connect to SMTP host
```

### 5. `testCurrentCodeFailsToSendEmail()` (Integration Test)
Confirms that the current code has multiple issues and cannot send email.

**Expected Counterexample**:
```
Exception thrown: false
Stderr output: "Lỗi gửi email xác nhận OTP: ..."
```

This confirms the overall bug: email sending does not work, and exceptions are swallowed.

## Expected Test Results on Unfixed Code

### CRITICAL: Test MUST FAIL

When running on unfixed code, we expect:

1. **BC2 Test**: FAILS with message "BC2 FAILED: Email sent successfully with leaked credentials - this should not happen!" OR catches exception (which is actually expected behavior, but current code swallows it)

2. **BC3 Test**: FAILS with message "BC3 FAILED: Exception was swallowed (only logged to stderr), not thrown to caller"

3. **BC4 Test**: May pass or fail depending on connection timeout

4. **Integration Test**: FAILS with message "INTEGRATION TEST FAILED: Exception was swallowed (only logged to stderr)"

### Expected Console Output

```
=== Bug Condition Exploration Results ===
Exception thrown: false
Exception message: null
Stderr output: Lỗi gửi email xác nhận OTP: ...
=========================================
```

## How to Run the Test

```bash
# Using Maven wrapper (if available)
./mvnw test -Dtest=EmailSendingBugConditionTest

# Using Maven directly
mvn test -Dtest=EmailSendingBugConditionTest

# Run all tests
./mvnw test
```

## After Fix Implementation

After implementing the fix (Task 3), this same test should be re-run. The expected behavior changes:

1. **BC1**: App should fail to start if credentials are empty (cannot test in runtime)
2. **BC2**: Should throw exception with clear authentication error message
3. **BC3**: Should throw `RuntimeException` wrapping `MessagingException`
4. **BC4**: Should use port 465 + SSL (standardized configuration)
5. **Integration**: Should throw exception (not swallow it)

The test will need to be updated to verify the fixed behavior, or new tests will be written to validate the fix.

## Validation Requirements

This test validates the following requirements from `bugfix.md`:

- **1.1**: Email verification OTP fails with connection timeout
- **1.2**: Password reset OTP fails with connection timeout
- **1.3**: Test endpoint fails with connection timeout
- **1.4**: MessagingException is swallowed (only logged)
- **1.5**: Gmail App Password is leaked and disabled
- **1.6**: Empty environment variables cause silent failures
- **1.7**: Credentials are hardcoded in application.properties

## Notes

- The test uses `@TestPropertySource` to configure email settings for testing
- Connection timeout is set to 5 seconds to speed up test execution
- The test captures stderr output to verify exception swallowing behavior
- The test is designed to be **observation-based**: it documents the current buggy behavior

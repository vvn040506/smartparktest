# Email Sending Preservation Property Tests Documentation

## Overview

This document describes the preservation property tests created for the Email Sending Connection Timeout Fix bugfix spec. These tests ensure that existing functionality is preserved when implementing the bug fix.

## Test File

**Location**: `src/test/java/com/smartpark/bugfix/EmailSendingPreservationTest.java`

## Critical Requirements

⚠️ **IMPORTANT**: These tests MUST PASS on unfixed code (confirms baseline behavior to preserve)

These tests follow the **observation-first methodology**:
1. Observe behavior on unfixed code for non-buggy inputs
2. Write property-based tests capturing observed behavior patterns
3. Verify tests pass on unfixed code (baseline)
4. After fix implementation, verify tests still pass (no regressions)

## Preservation Requirements Tested

### PR1: Email Content Format
**Property**: FOR ALL valid inputs, email body MUST contain correct OTP code and HTML formatting

**Test**: `testEmailContentFormatIsPreserved()`

**What it verifies**:
- Email method signatures remain unchanged
- MimeMessage creation works correctly
- OTP code format is 6 digits
- HTML template structure is preserved
- All 5 email methods exist with correct signatures:
  - `sendAccountVerificationOTP()`
  - `sendStaffResetOTP()`
  - `sendAccountVerificationEmail()`
  - `sendStaffResetEmail()`
  - `sendResetEmail()`

**Test approach**:
- Tests multiple OTP codes: "123456", "999999", "000000", "654321"
- Tests multiple email addresses and names
- Verifies method signatures using reflection
- Validates OTP format using regex `\d{6}`

---

### PR2: OTP Code Format and Expiry
**Property**: FOR ALL OTP generation calls, code length == 6 AND expiry == 10 minutes

**Test**: `testOtpCodeFormatAndExpiryIsPreserved()`

**What it verifies**:
- OTP code is exactly 6 digits
- OTP code is numeric (100000-999999 range)
- OTP expiry is 10 minutes from creation (±1 minute tolerance)
- OTP is not marked as used initially
- OTP attempts counter is 0 initially

**Test approach**:
- Generates 10 OTP codes to verify consistency
- Creates test StaffAccount in database
- Calls `createVerificationOTP()` multiple times
- Validates each OTP code format and expiry time
- Uses `Duration.between()` to calculate time difference
- Cleans up tokens between iterations

**Property-based testing**: Tests multiple generated OTP codes to ensure the property holds for all cases

---

### PR3: @Async Annotation
**Property**: FOR ALL async email calls, method returns before email is actually sent

**Test**: `testAsyncAnnotationIsPreserved()`

**What it verifies**:
- `sendVerificationEmailAsync()` method exists
- Method has `@Async` annotation
- Method returns quickly (< 1000ms) without blocking
- Async execution happens in background

**Test approach**:
- Uses reflection to verify `@Async` annotation presence
- Measures execution time of async method call
- Uses `CompletableFuture` to test non-blocking behavior
- Verifies method returns before email sending completes

**Why this matters**: Email sending should not block database transactions or HTTP requests

---

### PR4: Dependency Injection
**Property**: FOR ALL beans using EmailService, injection succeeds

**Test**: `testDependencyInjectionIsPreserved()`

**What it verifies**:
- `EmailService` is injected successfully
- `AccountVerificationService` is injected successfully
- `JavaMailSender` is injected successfully
- `EmailService` is injected into `AccountVerificationService`
- `EmailService` is a singleton (same instance everywhere)
- All required methods exist on `EmailService`

**Test approach**:
- Uses `@Autowired` fields to verify injection
- Calls `getEmailService()` to verify nested injection
- Uses `assertSame()` to verify singleton behavior
- Uses reflection to verify method existence

**Why this matters**: Spring dependency injection must continue to work after configuration changes

---

### PR5: UTF-8 Encoding and HTML Rendering
**Property**: FOR ALL emails with Vietnamese text, encoding == UTF-8 AND rendering correct

**Test**: `testUtf8EncodingAndHtmlRenderingIsPreserved()`

**What it verifies**:
- Vietnamese characters are preserved in method parameters
- MimeMessage creation works with Vietnamese text
- All email methods accept String parameters (HTML content)
- Method signatures remain unchanged

**Test approach**:
- Tests multiple Vietnamese names:
  - "Nguyễn Văn A"
  - "Trần Thị Bình"
  - "Lê Hoàng Châu"
  - "Phạm Minh Đức"
  - "Võ Thị Ê"
- Validates Vietnamese character presence using regex
- Verifies all email method signatures using reflection

**Why this matters**: The application serves Vietnamese users, so UTF-8 encoding is critical

---

### Integration Test
**Test**: `testAllPreservationRequirements()`

**What it verifies**:
- All 5 preservation requirements work together
- No conflicts between different requirements
- End-to-end preservation of baseline behavior

**Test approach**:
- Creates test account with Vietnamese name
- Generates OTP and validates format
- Verifies @Async annotation
- Verifies dependency injection
- Validates UTF-8 encoding
- Prints summary of all checks

---

## Test Configuration

The tests use `@TestPropertySource` to configure SMTP settings:

```java
@TestPropertySource(properties = {
    "spring.mail.host=smtp.gmail.com",
    "spring.mail.port=587",
    "spring.mail.username=test@example.com",
    "spring.mail.password=testpassword",
    "spring.mail.properties.mail.smtp.auth=true",
    "spring.mail.properties.mail.smtp.starttls.enable=true",
    "spring.mail.properties.mail.smtp.connectiontimeout=5000",
    "spring.mail.properties.mail.smtp.timeout=5000"
})
```

**Note**: These are test credentials that won't actually send emails. The tests verify behavior without requiring actual SMTP connection.

---

## Running the Tests

### Run all preservation tests:
```bash
./mvnw test -Dtest=EmailSendingPreservationTest
```

### Run specific test:
```bash
./mvnw test -Dtest=EmailSendingPreservationTest#testOtpCodeFormatAndExpiryIsPreserved
```

### Expected outcome on UNFIXED code:
✅ **ALL TESTS SHOULD PASS**

This confirms that baseline behavior is correctly captured and will be preserved during the fix.

### Expected outcome AFTER fix implementation:
✅ **ALL TESTS SHOULD STILL PASS**

This confirms that the fix did not introduce regressions.

---

## Test Cleanup

Each test includes cleanup in `@AfterEach`:
```java
@AfterEach
public void cleanup() {
    try {
        tokenRepository.deleteAll();
        staffAccountRepository.deleteAll();
    } catch (Exception e) {
        // Ignore cleanup errors
    }
}
```

This ensures tests don't interfere with each other.

---

## Property-Based Testing Approach

These tests use property-based testing principles:

1. **Universal properties**: Tests verify properties that should hold for ALL inputs
   - Example: "FOR ALL OTP codes, length == 6"

2. **Multiple test cases**: Tests generate/use multiple inputs to verify consistency
   - Example: Tests 10 different OTP codes to verify format is always 6 digits

3. **Invariants**: Tests verify invariants that should never change
   - Example: OTP expiry is always 10 minutes

4. **Behavioral properties**: Tests verify behavioral properties
   - Example: @Async methods always return quickly

---

## Relationship to Bug Condition Tests

These preservation tests complement the bug condition tests:

| Bug Condition Tests | Preservation Tests |
|---------------------|-------------------|
| MUST FAIL on unfixed code | MUST PASS on unfixed code |
| Verify bug exists | Verify baseline behavior |
| Test buggy inputs | Test non-buggy inputs |
| Confirm fix works | Confirm no regressions |

**Together**, these tests provide comprehensive coverage:
- Bug condition tests ensure the fix solves the problem
- Preservation tests ensure the fix doesn't break existing functionality

---

## Requirements Mapping

These tests validate the following requirements from `bugfix.md`:

- **3.1**: Email content format preserved
- **3.2**: OTP code format preserved (6 digits)
- **3.3**: OTP expiry preserved (10 minutes)
- **3.4**: Link format preserved
- **3.5**: @Async annotation preserved
- **3.6**: Dependency injection preserved
- **3.7**: UTF-8 encoding preserved
- **3.8**: Scheduled job not affected

---

## Success Criteria

✅ **Preservation tests are successful when**:
1. All tests pass on unfixed code (baseline captured)
2. All tests pass after fix implementation (no regressions)
3. Tests verify all 5 preservation requirements (PR1-PR5)
4. Tests use property-based testing approach
5. Tests are maintainable and well-documented

❌ **Preservation tests fail if**:
1. Any test fails on unfixed code (baseline not captured correctly)
2. Any test fails after fix (regression introduced)
3. Tests don't cover all preservation requirements
4. Tests are flaky or environment-dependent

---

## Maintenance Notes

When modifying email functionality in the future:

1. **Run preservation tests first** to establish baseline
2. **Make changes** to email code
3. **Run preservation tests again** to verify no regressions
4. **Update tests** if intentional behavior changes are made
5. **Document changes** in this file

---

## Related Files

- **Bug Condition Tests**: `EmailSendingBugConditionTest.java`
- **Bugfix Requirements**: `.kiro/specs/email-sending-connection-timeout-fix/bugfix.md`
- **Design Document**: `.kiro/specs/email-sending-connection-timeout-fix/design.md`
- **Tasks**: `.kiro/specs/email-sending-connection-timeout-fix/tasks.md`
- **Email Service**: `src/main/java/com/smartpark/service/EmailService.java`
- **Account Verification Service**: `src/main/java/com/smartpark/service/AccountVerificationService.java`

---

## Questions or Issues?

If tests fail unexpectedly:

1. Check if baseline behavior has changed
2. Verify test configuration is correct
3. Check database state (cleanup might have failed)
4. Review recent code changes
5. Consult the design document for expected behavior

---

**Last Updated**: 2026-04-07
**Author**: Kiro AI
**Status**: Ready for execution on unfixed code

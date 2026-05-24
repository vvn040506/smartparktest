# Email Sending Bug Condition Exploration Test

## Task 1: Write Bug Condition Exploration Test

**Status**: ✅ COMPLETED

**Created Files**:
1. `EmailSendingBugConditionTest.java` - Main test file
2. `BUG_CONDITION_TEST_DOCUMENTATION.md` - Detailed test documentation
3. `EXPECTED_COUNTEREXAMPLES.md` - Expected counterexamples and bug confirmations
4. `README.md` - This file

---

## Overview

This test is designed to **FAIL on unfixed code** to confirm that the bugs exist. It validates 4 bug conditions identified in the bugfix design:

### Bug Conditions Tested

1. **BC1: Empty Credentials** - Empty credentials should cause startup failure, not silent runtime failure
2. **BC2: Leaked Password** - Leaked App Password (`gvofejaenxpsylmo`) causes authentication failure
3. **BC3: Exception Swallowing** - Exceptions are swallowed and only logged to stderr
4. **BC4: Port Mismatch** - Inconsistent port configuration causes connection issues

---

## Test Structure

### Test Class: `EmailSendingBugConditionTest`

**Location**: `src/test/java/com/smartpark/bugfix/EmailSendingBugConditionTest.java`

**Test Methods**:
1. `testEmptyCredentialsShouldFail()` - Documents BC1
2. `testLeakedPasswordShouldFail()` - Tests BC2
3. `testExceptionsShouldNotBeSwallowed()` - Tests BC3
4. `testPortMismatchShouldFail()` - Tests BC4
5. `testCurrentCodeFailsToSendEmail()` - Integration test

**Configuration**:
```java
@SpringBootTest
@TestPropertySource(properties = {
    "spring.mail.host=smtp.gmail.com",
    "spring.mail.port=587",
    "spring.mail.username=vovietnhat1996@gmail.com",
    "spring.mail.password=gvofejaenxpsylmo",  // LEAKED PASSWORD
    "spring.mail.properties.mail.smtp.auth=true",
    "spring.mail.properties.mail.smtp.starttls.enable=true",
    "spring.mail.properties.mail.smtp.connectiontimeout=5000",
    "spring.mail.properties.mail.smtp.timeout=5000"
})
```

---

## How to Run the Test

### Option 1: Using Maven Wrapper
```bash
./mvnw test -Dtest=EmailSendingBugConditionTest
```

### Option 2: Using Maven Directly
```bash
mvn test -Dtest=EmailSendingBugConditionTest
```

### Option 3: Run All Tests
```bash
./mvnw test
```

### Option 4: Using IDE
- Open `EmailSendingBugConditionTest.java` in your IDE
- Right-click on the class or individual test methods
- Select "Run Test" or "Debug Test"

---

## Expected Test Results

### On Unfixed Code (Current State)

**CRITICAL**: Tests MUST FAIL to confirm bugs exist

Expected failures:

1. **`testLeakedPasswordShouldFail()`**:
   - May fail with: "BC2 FAILED: Email sent successfully with leaked credentials"
   - OR may catch exception (but current code swallows it, so this might not happen)

2. **`testExceptionsShouldNotBeSwallowed()`**:
   - FAILS with: "BC3 FAILED: Exception was swallowed (only logged to stderr), not thrown to caller"
   - Stderr output: "Lỗi gửi email xác nhận OTP: ..."

3. **`testPortMismatchShouldFail()`**:
   - May pass or fail depending on connection timeout
   - Expected: Connection timeout or authentication failure

4. **`testCurrentCodeFailsToSendEmail()`** (Integration):
   - FAILS with: "INTEGRATION TEST FAILED: Exception was swallowed (only logged to stderr)"
   - Confirms overall bug: email sending does not work

### Expected Console Output

```
=== Bug Condition Exploration Results ===
Exception thrown: false
Exception message: null
Stderr output: Lỗi gửi email xác nhận OTP: Could not connect to SMTP host: smtp.gmail.com, port: 587; nested exception is: java.net.SocketTimeoutException: Connection timed out
=========================================

INTEGRATION TEST FAILED: Exception was swallowed (only logged to stderr). 
This confirms BC3 (exception swallowing). 
Expected: RuntimeException thrown to caller. 
Actual: No exception, stderr: Lỗi gửi email xác nhận OTP: ...
```

---

## After Fix Implementation (Task 3)

After implementing the fix, re-run this test. Expected changes:

### Expected Behavior After Fix

1. **BC1**: App should fail to start if credentials are empty (cannot test in runtime)
2. **BC2**: Should throw exception with clear authentication error message (no hardcoded leaked password)
3. **BC3**: Should throw `RuntimeException` wrapping `MessagingException` (not swallow)
4. **BC4**: Should use port 465 + SSL (standardized configuration)
5. **Integration**: Should throw exception (not swallow it)

### Test Updates Needed

The test will need to be updated in Task 3.7 to verify the fixed behavior:
- Update test configuration to use valid credentials (from environment variables)
- Update assertions to expect exceptions to be thrown (not swallowed)
- Verify email sending succeeds with valid configuration

---

## Validation Requirements

This test validates the following requirements from `bugfix.md`:

- **1.1**: Email verification OTP fails with connection timeout
- **1.2**: Password reset OTP fails with connection timeout
- **1.3**: Test endpoint fails with connection timeout
- **1.4**: MessagingException is swallowed (only logged)
- **1.5**: Gmail App Password is leaked and disabled
- **1.6**: Empty environment variables cause silent failures
- **1.7**: Credentials are hardcoded in application.properties

---

## Documentation Files

1. **`BUG_CONDITION_TEST_DOCUMENTATION.md`**:
   - Detailed explanation of each bug condition
   - Test method descriptions
   - Expected test results
   - How to run the test

2. **`EXPECTED_COUNTEREXAMPLES.md`**:
   - Expected counterexamples for each bug condition
   - Actual vs expected behavior
   - Bug confirmation criteria
   - Summary table of bugs confirmed

3. **`README.md`** (this file):
   - Quick overview and guide
   - How to run the test
   - Expected results
   - Next steps

---

## Next Steps

1. ✅ **Task 1 COMPLETED**: Bug condition exploration test created
2. ⏭️ **Task 2**: Write preservation property tests (BEFORE implementing fix)
3. ⏭️ **Task 3**: Implement the fix
4. ⏭️ **Task 3.7**: Re-run this test to verify fix works
5. ⏭️ **Task 3.8**: Verify preservation tests still pass

---

## Notes

- The test uses `@TestPropertySource` to configure email settings for testing
- Connection timeout is set to 5 seconds to speed up test execution
- The test captures stderr output to verify exception swallowing behavior
- The test is designed to be **observation-based**: it documents the current buggy behavior
- **DO NOT attempt to fix the test or the code when it fails** - failure confirms bugs exist

---

## Troubleshooting

### Maven Wrapper Not Found

If you get an error about Maven wrapper not found:
```
Error: Could not find or load main class org.apache.maven.wrapper.MavenWrapperMain
```

**Solution**: Use Maven directly instead:
```bash
mvn test -Dtest=EmailSendingBugConditionTest
```

### Maven Not Installed

If Maven is not installed, you can:
1. Install Maven: https://maven.apache.org/install.html
2. Use your IDE to run the test (IntelliJ IDEA, Eclipse, VS Code with Java extensions)

### Test Takes Too Long

The test has connection timeouts set to 5 seconds. If it still takes too long:
- Check your network connection
- The timeout is expected (confirms the bug)
- Wait for the test to complete (should finish within 30-60 seconds)

---

## Contact

For questions or issues with this test, refer to:
- Bugfix spec: `.kiro/specs/email-sending-connection-timeout-fix/bugfix.md`
- Design doc: `.kiro/specs/email-sending-connection-timeout-fix/design.md`
- Tasks: `.kiro/specs/email-sending-connection-timeout-fix/tasks.md`

# Authentication Tests - Quick Reference Guide

## Test Structure Overview

```
Authentication Feature Tests
├── Data Layer (Repository)
│   └── AuthRepositoryImplTest.kt (12 tests)
│       ├── Login behavior with Firebase mocking
│       ├── Sign-up with Firestore integration
│       └── Google sign-in error handling
│
├── Domain Layer (Use Cases)
│   ├── SignUpUseCaseTest.kt (5 tests)
│   │   └── Verification of repository delegation
│   │
│   └── GoogleSignInUseCaseTest.kt (7 tests)
│       └── Google auth business logic
│
└── Presentation Layer (ViewModel)
    ├── AuthViewModelTest.kt - Unit (13 tests)
    │   ├── LiveData state management
    │   └── Loading state transitions
    │
    ├── AuthViewModelTest.kt - Instrumented (11 tests)
    │   ├── Android framework integration
    │   └── Real LiveData observer behavior
    │
    └── ExampleInstrumentedTest.kt (1 test)
        └── Basic test infrastructure validation
```

**Total: 49 Tests across 6 test files**

---

## Test Categories

### Repository Tests (Data Layer)
**Purpose:** Verify Firebase authentication and Firestore database interactions

**Tests:**
- Login with valid/invalid credentials
- Login error handling and null user checks
- Sign-up with complete flow (auth + Firestore)
- Google sign-in for new and existing users
- Exception propagation and error wrapping

**Tools:** Firebase mocking, MockK, Result types

### Use Case Tests (Domain Layer)
**Purpose:** Verify business logic delegation to repository

**SignUpUseCase:**
- Repository delegation verification
- Error propagation from repository
- Parameter passing accuracy

**GoogleSignInUseCase:**
- Token delegation to repository
- Error handling
- Client creation

**Tools:** MockK coroutine mocking, coEvery/coVerify

### ViewModel Tests (Presentation Layer)
**Purpose:** Verify UI state management and user interaction handling

**LiveData Testing:**
- Loading state transitions (false → true → false)
- Failure results posted to LiveData
- Observer pattern with proper cleanup

**Unit Version:**
- Uses test dispatcher for controlled coroutine execution
- InstantTaskExecutorRule for LiveData immediate execution
- Observer pattern for state capture

**Instrumented Version:**
- Runs on actual Android device/emulator
- Tests real Android framework LiveData behavior
- Additional error case coverage

---

## Test Patterns Used

### 1. Arrange-Act-Assert (AAA)
```kotlin
@Test
fun login_withValidCredentials_shouldCallRepository() = runTest {
    // ARRANGE - Setup mocks and test data
    val email = "test@example.com"
    val password = "password123"
    coEvery { mockAuthRepository.login(email, password) } returns Result.success(Unit)
    
    // ACT - Execute the code being tested
    authViewModel.login(email, password)
    testDispatcher.scheduler.advanceUntilIdle()
    
    // ASSERT - Verify the result
    coVerify { mockAuthRepository.login(email, password) }
}
```

### 2. LiveData Observer Capture
```kotlin
@Test
fun login_shouldSetLoadingStateToTrue() = runTest {
    // Capture all state emissions
    val loadingStates = mutableListOf<Boolean>()
    val observer = Observer<Boolean> { loadingStates.add(it) }
    
    authViewModel.isLoading.observeForever(observer)
    authViewModel.login(email, password)
    testDispatcher.scheduler.advanceUntilIdle()
    
    // Verify state transitions occurred
    assertTrue(loadingStates.any { it })
    
    // Always cleanup
    authViewModel.isLoading.removeObserver(observer)
}
```

### 3. Error Scenario Testing
```kotlin
@Test
fun login_whenRepositoryFails_shouldReturnFailure() = runTest {
    // Setup mock to return failure
    val exception = Exception("Invalid credentials")
    coEvery { mockAuthRepository.login(email, password) } returns Result.failure(exception)
    
    // Execute and verify failure propagation
    val result = authViewModel.login(email, password)
    
    // Assert failure details
    assertTrue(result.isFailure)
    assertEquals(exception, result.exceptionOrNull())
}
```

### 4. Task Mocking (Firebase)
```kotlin
@Test
fun login_withValidCredentials_shouldReturnSuccess() = runTest {
    // Mock Firebase AuthResult
    every { mockAuthResult.user } returns mockFirebaseUser
    
    // Mock Firebase Task
    val mockTask = mockk<Task<AuthResult>>()
    coEvery { mockTask.await() } returns mockAuthResult
    coEvery { mockAuth.signInWithEmailAndPassword(email, password) } returns mockTask
    
    // Test implementation
    val result = authRepository.login(email, password)
    assertTrue(result.isSuccess)
}
```

---

## Key Testing Utilities

### MockK Functions
| Function | Purpose |
|----------|---------|
| `mockk()` | Create a mock object |
| `coEvery { ... } returns ...` | Mock async function return |
| `coEvery { ... } throws ...` | Mock async function exception |
| `coVerify { ... }` | Verify async function was called |
| `every { ... } returns ...` | Mock regular function return |
| `coVerify(exactly = n)` | Verify called exactly n times |

### Test Annotations
| Annotation | Purpose |
|-----------|---------|
| `@Before` | Setup before each test |
| `@After` | Cleanup after each test |
| `@Test` | Mark as test method |
| `@get:Rule` | Apply test rule (InstantTaskExecutorRule) |
| `@RunWith(AndroidJUnit4::class)` | Run as instrumented test |

### Coroutine Testing
| Function | Purpose |
|----------|---------|
| `runTest { }` | Modern coroutine test runner |
| `runBlocking { }` | Legacy coroutine test runner |
| `StandardTestDispatcher()` | Controllable test dispatcher |
| `advanceUntilIdle()` | Run all pending coroutines |
| `setMain(dispatcher)` | Set Dispatchers.Main for testing |

---

## Expected Test Results

All 49 tests should pass:

```
✓ AuthRepositoryImplTest (12 tests)
  ✓ Login with valid credentials returns success
  ✓ Login with Firebase exception returns failure
  ✓ Login when user is null returns failure
  ✓ SignUp with valid data returns success
  ✓ SignUp when auth creation fails returns failure
  ✓ SignUp when user is null returns failure
  ✓ Google sign-in with valid token returns success
  ✓ Google sign-in with invalid token returns failure
  ✓ Google sign-in when user is null returns failure

✓ SignUpUseCaseTest (5 tests)
  ✓ Delegates to repository with valid data
  ✓ Propagates repository failure
  ✓ Propagates repository exception
  ✓ Works correctly with multiple invocations
  ✓ Passes all parameters exactly to repository

✓ GoogleSignInUseCaseTest (7 tests)
  ✓ Delegates to repository with valid token
  ✓ Returns failure with invalid token
  ✓ Returns failure with empty token
  ✓ Propagates repository exception
  ✓ Creates Google sign-in client
  ✓ Works with multiple tokens
  ✓ Passes token exactly to repository

✓ AuthViewModelTest - Unit (13 tests)
  ✓ Login calls repository
  ✓ Login sets loading to true
  ✓ Login sets loading to false after completion
  ✓ Login posts failure result
  ✓ Empty email calls repository
  ✓ Empty password calls repository
  ✓ Google sign-in calls repository
  ✓ Google sign-in sets loading state
  ✓ Google sign-in posts failure
  ✓ Loading state false after completion (Google)
  ✓ Login completes without exception
  ✓ Google sign-in completes without exception

✓ AuthViewModelTest - Instrumented (11 tests)
  ✓ Login calls repository
  ✓ Login captures loading state transitions
  ✓ Login posts failure result to LiveData
  ✓ Empty email calls repository
  ✓ Empty password calls repository
  ✓ Google sign-in calls repository
  ✓ Google sign-in sets loading state
  ✓ Google sign-in posts failure
  ✓ Loading state captures transitions
  ✓ Loading state false after completion
  ✓ Loading false even on error

✓ ExampleInstrumentedTest (1 test)
  ✓ App context has correct package name

TOTAL: 49 tests ✓
```

---

## Common Issues & Solutions

### Issue: Test fails with "Unmatched invocation"
**Solution:** Ensure mock setup matches exact parameters or use `any()`
```kotlin
// ❌ Wrong - parameter doesn't match
coEvery { mockRepo.login("test@example.com", "123") } returns Result.success(Unit)
authViewModel.login("test@example.com", "wrong")  // Different parameter!

// ✅ Right - use any() for flexible matching
coEvery { mockRepo.login(any(), any()) } returns Result.success(Unit)
```

### Issue: LiveData observer not capturing state
**Solution:** Remember to call `observeForever()` before action
```kotlin
// ❌ Wrong - observer added after state changes
authViewModel.login(...)
authViewModel.isLoading.observeForever(observer)

// ✅ Right - observer before action
authViewModel.isLoading.observeForever(observer)
authViewModel.login(...)
```

### Issue: Test times out or hangs
**Solution:** Always call `advanceUntilIdle()` to run pending coroutines
```kotlin
authViewModel.login(email, password)
testDispatcher.scheduler.advanceUntilIdle()  // ← Don't forget this
```

### Issue: "No android jar file"
**Solution:** Run instrumented tests on device/emulator
```bash
./gradlew connectedAndroidTest  # Not ./gradlew test
```

---

## Maintenance Guide

### When Adding New Auth Features

1. **Add repository test** - Test Firebase interaction
2. **Add use case test** - Test business logic
3. **Add ViewModel test** - Test UI state management
4. **Update this guide** - Keep documentation current

### When Modifying Existing Tests

- Keep the AAA pattern
- Maintain descriptive names
- Update documentation
- Run full test suite: `./gradlew test connectedAndroidTest`

### Code Coverage Goal

Aim for:
- **Data Layer:** 90%+ coverage (all Firebase operations)
- **Domain Layer:** 100% coverage (pure business logic)
- **Presentation Layer:** 85%+ coverage (LiveData state is testable)

---

## Related Files

- `app/src/test/` - Unit tests (run on JVM)
- `app/src/androidTest/` - Instrumented tests (run on Android)
- `app/src/main/java/.../auth/` - Authentication source code
- `build.gradle.kts` - Test dependencies and configuration

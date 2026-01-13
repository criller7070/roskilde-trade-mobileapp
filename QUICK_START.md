# Firebase Authentication - Quick Start Checklist

## Pre-Implementation Setup

- [ ] Download `google-services.json` from Firebase Console
- [ ] Add `google-services.json` to `app/` directory
- [ ] Add to `.gitignore`: `app/google-services.json`

## Dependencies Configuration

### `build.gradle.kts` (Project level)
- [ ] Add Google Services plugin:
  ```kotlin
  plugins {
      id("com.google.gms.google-services") version "4.3.15" apply false
  }
  ```

### `build.gradle.kts` (App level)
- [ ] Apply Google Services plugin:
  ```kotlin
  plugins {
      id("com.google.gms.google-services")
  }
  ```

- [ ] Add dependencies:
  ```kotlin
  implementation(libs.firebase.auth)
  implementation(libs.firebase.firestore)
  implementation(libs.firebase.storage)
  implementation(libs.firebase.functions)
  implementation(libs.play.services.auth)  // For Google Sign-In
  ```

## Firebase Configuration

- [ ] Initialize Firebase in your Application class or MainActivity:
  ```kotlin
  override fun onCreate() {
      super.onCreate()
      FirebaseInitializer.initialize(this)
  }
  ```

- [ ] Configure Firestore Security Rules in Firebase Console:
  ```firestore
  rules_version = '2';
  service cloud.firestore {
    match /databases/{database}/documents {
      match /users/{userId} {
        allow read, write: if request.auth.uid == userId;
      }
      match /items/{document=**} {
        allow read: if true;
        allow write, delete: if request.auth != null;
      }
    }
  }
  ```

- [ ] Enable Email/Password authentication in Firebase Console → Authentication → Sign-in method

- [ ] Configure Google Sign-In:
  - [ ] Go to Firebase Console → Project Settings
  - [ ] Download OAuth 2.0 Client IDs
  - [ ] Update web client ID in `GoogleSignInOptions` (currently empty)

## UI Integration

### Fragment Setup
Your existing Fragment-based login UI (`LoginFragment.kt`, `LoginRequiredFragment.kt`) now works with the new authentication system.

- Update `LoginFragment.kt` to inject `AuthViewModel` or existing `LoginViewModel`
- Wire existing `LoginRequiredFragment.kt` to use new auth state
- Both are located in `app/src/main/java/dk/rosswap/mobile/feature/auth/presentation/`

### Observe Auth State
In your Fragments, observe the auth state:
```kotlin
authViewModel.authState.observe(viewLifecycleOwner) { state ->
    when (state) {
        is AuthState.Loading -> showLoadingView()
        is AuthState.Authenticated -> navigateToHome()
        is AuthState.Unauthenticated -> showLoginView()
        is AuthState.Error -> showErrorMessage(state.exception.message)
    }
}
```

### Google Sign-In Setup
When implementing Google Sign-In:
```kotlin
val launcher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            authViewModel.signInWithGoogle(account?.idToken ?: "")
        } catch (e: ApiException) {
            showError(e.message)
        }
    }
}
```

## Testing

### Email Validation Testing
- [ ] Test valid emails: `user@gmail.com`, `user@example.dk`
- [ ] Test invalid formats: `invalid@@email`, `user@`, `@example.com`
- [ ] Test disposable emails: `user@tempmail.org`, `user@mailinator.com`
- [ ] Test suspicious patterns: `test@example.com`, `fake@domain.com`

### Authentication Flow Testing
- [ ] Test email/password signup
- [ ] Test email/password login
- [ ] Test invalid credentials error handling
- [ ] Test duplicate email error handling
- [ ] Test Google Sign-In flow
- [ ] Test logout functionality
- [ ] Test protected route access when unauthenticated

### State Management Testing
- [ ] Verify `AuthState.Loading` shows during auth operations
- [ ] Verify `AuthState.Authenticated` contains enriched user data
- [ ] Verify `AuthState.Unauthenticated` on logout
- [ ] Verify `AuthState.Error` displays error messages

## Production Deployment

- [ ] Remove debug logging from production builds
- [ ] Configure Firestore backup policies
- [ ] Enable Firebase Authentication rate limiting
- [ ] Set up Firebase Monitoring and Analytics
- [ ] Review and test Firestore Security Rules
- [ ] Configure email templates in Firebase Console
- [ ] Set up password reset flow
- [ ] Enable two-factor authentication (optional)

## Documentation Review

- [ ] Read `AUTHENTICATION_GUIDE.md` for complete setup instructions
- [ ] Review `app/src/main/java/dk/rosswap/mobile/feature/auth/README.md` for architecture details
- [ ] Check inline code comments for implementation details
- [ ] Reference code examples in guide for common scenarios

## Troubleshooting

| Issue | Checklist |
|-------|-----------|
| Google Sign-In fails | ✓ Web client ID configured ✓ `google-services.json` updated ✓ OAuth scope includes email |
| Firestore permission denied | ✓ Security rules updated ✓ User authenticated ✓ Document path correct |
| Email validation too strict | ✓ Add domain to `LEGITIMATE_DOMAINS` ✓ Check `EmailValidator.kt` |
| Auth state not updating | ✓ `AuthViewModel` properly injected ✓ `hiltViewModel()` used ✓ Listener set up correctly |
| Signup fails | ✓ Password >= 6 chars ✓ Valid email ✓ GDPR consent checked ✓ Email not already registered |

## Files Created/Updated

| File | Purpose |
|------|---------|
| `FirebaseInitializer.kt` | Firebase services initialization |
| `GoogleSignInHelper.kt` | Google Sign-In implementation |
| `AuthRepositoryImpl.kt` | Authentication repository (updated) |
| `SignUpUseCase.kt` | Email/password signup use case |
| `GoogleSignInUseCase.kt` | Google Sign-In use case |
| `AuthRepository.kt` | Auth interface (updated) |
| `AuthViewModel.kt` | State management (updated, uses LiveData) |
| `LoginFragment.kt` | Fragment-based login UI (existing) |
| `LoginRequiredFragment.kt` | Fragment-based protected route (existing) |
| `EmailValidator.kt` | Email validation utility (in core/utils) |
| `AuthModule.kt` | Hilt DI configuration (updated) |

## Quick Test Commands

### Check Firestore (Firebase Console)
```
1. Go to Firestore Database
2. Create a test user via signup
3. Verify user document in "users" collection
4. Check fields: uid, name, email, gdprConsent, createdAt
```

### Monitor Authentication (Firebase Console)
```
1. Go to Authentication → Users tab
2. Verify new users appear after signup
3. Check email/password and Google Sign-In methods are listed
4. Review sign-in activity over time
```

### Debug Logs (Android Studio)
```
Filter: AuthViewModel
Filter: AuthRepositoryImpl
Filter: GoogleSignInHelper
Filter: EmailValidator
```

## Additional Resources

- [Firebase Authentication Docs](https://firebase.google.com/docs/auth)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [Jetpack Compose Docs](https://developer.android.com/jetpack/compose/documentation)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)

---

**Created:** January 12, 2025
**Status:** ✅ Ready to implement
**Estimated Setup Time:** 30-45 minutes
**Testing Time:** 30-60 minutes

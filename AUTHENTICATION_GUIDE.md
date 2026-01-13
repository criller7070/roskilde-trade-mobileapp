# Firebase Authentication System Implementation Guide

## Overview

This guide explains how to implement and use the Firebase authentication system for the Android mobile app. The system mirrors the web app's authentication architecture, providing email/password signup, Google Sign-In, and GDPR consent tracking.

## File Structure

```
app/src/main/java/dk/rosswap/mobile/feature/auth/
├── data/
│   ├── FirebaseInitializer.kt         # Firebase services initialization
│   ├── GoogleSignInHelper.kt          # Google Sign-In implementation
│   └── AuthRepositoryImpl.kt           # Authentication repository
├── domain/
│   ├── AuthRepository.kt              # Auth repository interface
│   ├── SignUpUseCase.kt               # Email/password signup use case
│   ├── GoogleSignInUseCase.kt         # Google Sign-In use case
│   └── EnrichUserUseCase.kt           # User data enrichment (existing)
├── presentation/
│   ├── AuthViewModel.kt               # Main auth state management ViewModel
│   ├── LoginViewModel.kt              # Login ViewModel (existing)
│   ├── LoginFragment.kt               # Login UI (Fragment-based)
│   ├── LoginRequiredFragment.kt       # Protected route fallback
│   └── LoginRequiredModelView.kt      # Model for LoginRequired
├── di/
│   └── AuthModule.kt                  # Hilt dependency injection
└── README.md                          # Technical documentation
```

**Note:** Email validation uses `core.utils.EmailValidator` (shared utility)

## Setup Steps

### 1. Google Services Configuration

**Ensure `google-services.json` is in place:**
- Download from Firebase Console: Project Settings → Download google-services.json
- Place in: `app/google-services.json` (not in version control)

**Add to `build.gradle` (Project level):**
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.3.15" apply false
}
```

**Add to `build.gradle.kts` (App level):**
```kotlin
plugins {
    id("com.google.gms.google-services")
}

dependencies {
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.functions)
    implementation(libs.play.services.auth) // For Google Sign-In
}
```

### 2. Initialize Firebase in Your Application Class

```kotlin
// In your Application or MainActivity onCreate()
FirebaseInitializer.initialize(context)
```

### 3. Configure Firebase Security Rules

Set up Firestore security rules to allow user document creation:

```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow users to read/write their own documents
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId;
    }
    
    // Allow anonymous reads for public data
    match /items/{document=**} {
      allow read: if true;
      allow write, delete: if request.auth != null;
    }
  }
}
```

## Usage Guide

### Scenario 1: Email/Password Signup

```kotlin
@Composable
fun SignupFlow() {
    val authViewModel = hiltViewModel<AuthViewModel>()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var hasConsented by remember { mutableStateOf(false) }

    when (authState) {
        is AuthState.Loading -> CircularProgressIndicator()
        is AuthState.Authenticated -> {
            // Navigate to home
            LaunchedEffect(Unit) {
                navController.navigate("home")
            }
        }
        is AuthState.Error -> {
            Text("Error: ${(authState as AuthState.Error).exception.message}")
        }
        else -> {
            Button(
                onClick = {
                    authViewModel.signUp(
                        email = email,
                        password = password,
                        name = name,
                        hasConsent = hasConsented
                    )
                }
            ) {
                Text("Sign Up")
            }
        }
    }
}
```

### Scenario 2: Email/Password Login

```kotlin
@Composable
fun LoginFlow() {
    val authViewModel = hiltViewModel<AuthViewModel>()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Button(
        onClick = {
            authViewModel.login(email, password)
        }
    ) {
        Text("Login")
    }
}
```

### Scenario 3: Google Sign-In

```kotlin
@Composable
fun GoogleSignInFlow() {
    val authViewModel = hiltViewModel<AuthViewModel>()
    val context = LocalContext.current
    
    // Get the Google Sign-In client
    val googleSignInClient = remember {
        GoogleSignInHelper(context).googleSignInClient
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    authViewModel.signInWithGoogle(idToken)
                }
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Sign-in failed", e)
            }
        }
    }

    Button(
        onClick = {
            val signInIntent = googleSignInClient.signInIntent
            launcher.launch(signInIntent)
        }
    ) {
        Text("Sign in with Google")
    }
}
```

### Scenario 4: Protected Routes

```kotlin
@Composable
fun ProtectedRoute() {
    val authViewModel = hiltViewModel<AuthViewModel>()
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    when (authState) {
        is AuthState.Authenticated -> {
            MainContent()
        }
        is AuthState.Unauthenticated -> {
            LoginRequiredScreen(navController)
        }
        is AuthState.Loading -> {
            LoadingScreen()
        }
        is AuthState.Error -> {
            ErrorScreen()
        }
    }
}
```

## Email Validation

The email validator provides comprehensive validation:

```kotlin
// Validate an email
val result = EmailValidator.validateEmail("user@example.com")
if (result.isValid) {
    println("Valid email!")
} else {
    println("Invalid: ${result.message}")
}

// Quick check
if (EmailValidator.isValidEmail("user@example.com")) {
    // proceed
}
```

**Validation checks:**
- ✓ Valid email format (RFC 5322 simplified)
- ✓ Known legitimate email providers
- ✓ Valid TLD (country codes, com, org, etc.)
- ✗ Blocks disposable/temporary email services
- ✗ Blocks suspicious patterns (test@, fake@, etc.)
- ✗ Requires reasonable domain structure

## Firestore Data Model

User documents are stored in the `users` collection:

```kotlin
data class User(
    val uid: String,                    // Firebase Auth UID
    val name: String,                   // Display name
    val email: String,                  // Email address
    val photoURL: String,               // Profile photo URL
    val createdAt: Date?,               // Account creation timestamp
    val gdprConsent: Boolean,           // GDPR consent flag
    val consentedAt: Date?,             // When consent was given
    val likedItemIds: List<String>,     // Items user liked
    val dislikedItemIds: List<String>,  // Items user disliked
    val isAnonymous: Boolean            // Anonymous user flag
)
```

Example Firestore document:
```json
{
  "uid": "abc123xyz",
  "name": "John Doe",
  "email": "john@example.com",
  "photoURL": "https://storage.googleapis.com/...",
  "createdAt": "2024-01-12T10:30:00Z",
  "gdprConsent": true,
  "consentedAt": "2024-01-12T10:30:00Z",
  "likedItemIds": ["item1", "item2"],
  "dislikedItemIds": ["item3"]
}
```

## State Management

The `AuthState` sealed class represents all possible authentication states:

```kotlin
sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val exception: Exception) : AuthState()
}
```

## Error Handling

The system handles errors gracefully:

```kotlin
when (authState) {
    is AuthState.Error -> {
        val error = (authState as AuthState.Error).exception
        when {
            error.message?.contains("email") == true -> 
                Toast.makeText(context, "Invalid email", Toast.LENGTH_SHORT).show()
            error.message?.contains("password") == true -> 
                Toast.makeText(context, "Invalid password", Toast.LENGTH_SHORT).show()
            else -> 
                Toast.makeText(context, "An error occurred", Toast.LENGTH_SHORT).show()
        }
    }
}
```

## Common Firebase Error Codes

| Code | Message | Solution |
|------|---------|----------|
| `auth/user-not-found` | No user with this email | Direct to signup |
| `auth/wrong-password` | Incorrect password | Suggest password reset |
| `auth/invalid-email` | Invalid email format | Ask for valid email |
| `auth/weak-password` | Password < 6 chars | Require stronger password |
| `auth/email-already-in-use` | Email registered | Direct to login |
| `auth/network-request-failed` | Network error | Retry or show offline msg |
| `auth/popup-blocked` | Popup was blocked | Inform user, retry |

## Testing

### Unit Tests

```kotlin
@Test
fun testEmailValidation() {
    val result = EmailValidator.validateEmail("invalid@@email.com")
    assertFalse(result.isValid)
    assertEquals("INVALID_FORMAT", result.reason)
}

@Test
fun testDisposableEmailBlocked() {
    val result = EmailValidator.validateEmail("test@tempmail.org")
    assertFalse(result.isValid)
    assertEquals("DISPOSABLE_EMAIL", result.reason)
}
```

### Integration Tests

```kotlin
@Test
fun testSignUp() = runTest {
    val repository = AuthRepositoryImpl(auth, firestore, googleSignInHelper)
    val result = repository.signUp("test@example.com", "password123", "Test User", true)
    assertTrue(result.isSuccess)
}
```

## Migration from Web App Patterns

| Web App Pattern | Android Implementation |
|-----------------|----------------------|
| React Context API | Hilt DI + ViewModel |
| `firebase.js` | `FirebaseInitializer.kt` |
| `AuthContext.useAuth()` | `hiltViewModel<AuthViewModel>()` |
| `onAuthStateChanged()` | `authStateListener` in ViewModel |
| `signInWithPopup()` | `GoogleSignInHelper.signInWithGoogle()` |
| State updates → re-render | `StateFlow` → Compose recomposition |
| Error popup | `Toast` or `Snackbar` |

## Security Best Practices

1. **Never commit `google-services.json`** - Add to `.gitignore`
2. **GDPR Consent** - Always track when users consent
3. **Email Validation** - Prevent fake/temporary emails
4. **Password Requirements** - Enforce minimum 6 characters
5. **Error Messages** - Don't expose sensitive details
6. **Firestore Rules** - Restrict user access properly
7. **Graceful Degradation** - Don't block auth if Firestore fails

## Troubleshooting

### Google Sign-In Returns Null idToken
**Cause:** Web client ID not configured
**Fix:** 
1. Go to Firebase Console → Project Settings
2. Ensure OAuth 2.0 Client ID (Web) is created
3. Update `GoogleSignInOptions` with correct ID

### Firestore Permission Denied
**Cause:** Security rules too restrictive
**Fix:**
```firestore
match /users/{userId} {
  allow write: if request.auth.uid == userId;
}
```

### Email Validation Too Strict
**Cause:** Custom domain not in whitelist
**Fix:** Add to `LEGITIMATE_DOMAINS` in `EmailValidator.kt`

### Auth State Never Updates
**Cause:** AuthViewModel not properly injected
**Fix:** Ensure `@HiltViewModel` annotation and `hiltViewModel()` in Compose

## Next Steps

1. **Implement password reset** - Add forgot password screen
2. **Phone authentication** - For OTP-based auth
3. **Biometric authentication** - Fingerprint/face unlock
4. **Two-factor authentication** - Enhanced security
5. **Social login** - Facebook, Apple ID, etc.
6. **Email verification** - Confirm user ownership
7. **User profile updates** - Allow name/photo changes

## Resources

- [Firebase Auth Documentation](https://firebase.google.com/docs/auth)
- [Google Sign-In for Android](https://developers.google.com/identity/sign-in/android)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/start)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)

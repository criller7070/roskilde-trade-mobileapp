# Firebase Authentication System for Android

This directory contains a complete Firebase authentication system that mirrors the web app's authentication architecture, implemented in Kotlin for Android.

## Architecture Overview

The authentication system follows a clean architecture pattern with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│              Presentation Layer (Fragments)                 │
│        (LoginFragment, LoginRequiredFragment)               │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              Presentation Layer (ViewModels)                │
│            (AuthViewModel, LoginViewModel)                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│         Domain Layer (Use Cases & Repositories)             │
│  SignUpUseCase, GoogleSignInUseCase, EnrichUserUseCase,     │
│  AuthRepository (Interface)                                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│         Data Layer (Firebase Integration)                   │
│  AuthRepositoryImpl, GoogleSignInHelper, FirebaseInitializer│
└─────────────────────────────────────────────────────────────┘
```

## Key Components

### Data Layer (`data/`)

#### `FirebaseInitializer.kt`
- Initializes Firebase services (Auth, Firestore, Storage, Functions)
- Mirrors the web app's `firebase.js`
- Provides singleton access to Firebase instances
- Configures regional settings for Cloud Functions

#### `GoogleSignInHelper.kt`
- Manages Google Sign-In flow
- Creates/updates user documents in Firestore with GDPR consent tracking
- Handles migration of existing users (ensuring GDPR fields are present)
- Mirrors the web app's `signInWithGoogle()` and `createGoogleUser()` functions

#### `AuthRepositoryImpl.kt`
- Implements the `AuthRepository` interface
- Handles email/password login and signup
- Manages Google Sign-In integration
- Enriches user data from Firestore (fetch display name, photo, GDPR consent, etc.)
- Gracefully handles Firestore errors to prevent auth blocking

### Domain Layer (`domain/`)

#### `AuthRepository.kt` (Interface)
- Defines contracts for authentication operations
- Methods: `login()`, `signUp()`, `signInWithGoogle()`, `enrichUserWithFirestoreData()`

#### `SignUpUseCase.kt`
- Use case for email/password signup
- Calls `firebaseSignUp()` which creates user in Firebase Auth and Firestore
- Mirrors the web app's form validation and user creation flow

#### `GoogleSignInUseCase.kt`
- Use case for Google Sign-In
- Provides access to the Google Sign-In client
- Orchestrates the Google authentication flow

### Presentation Layer (`presentation/`)

#### `AuthViewModel.kt`
- Manages auth state as `StateFlow<AuthState>`
- Listens to Firebase auth state changes
- Enriches user data from Firestore asynchronously
- Provides methods: `login()`, `signUp()`, `signOut()`
- Mirrors the web app's `AuthContext` pattern using Kotlin StateFlow

### UI Layer (`presentation/ui/`)

#### `SignupScreen.kt`
- Jetpack Compose signup UI
- Email validation with comprehensive checks
- GDPR consent checkbox
- Password strength validation
- Google Sign-In button
- Mirrors the web app's `Signup.jsx` component

#### `LoginScreen.kt`
- Jetpack Compose login UI
- Email/password authentication
- Link to signup page
- Mirrors a typical login experience

#### `LoginRequiredScreen.kt`
- Shown when accessing protected routes without authentication
- Navigation options to login or signup
- Mirrors the web app's `LoginRequired.jsx` component

### Utilities (`utils/`)

#### `EmailValidator.kt`
- Comprehensive email validation
- Checks legitimate email providers (lists major, Danish, and European providers)
- Blocks disposable/temporary email services
- Detects suspicious patterns
- Validates TLD and domain structure
- Returns detailed validation messages in Danish
- Mirrors the web app's `emailValidation.js`

### Dependency Injection (`di/`)

#### `AuthModule.kt`
- Hilt DI configuration
- Provides Firebase Auth, Firestore, GoogleSignInHelper, AuthRepository
- Singleton scope ensures single instances across the app

## State Management

### AuthState
The authentication state is managed through a sealed class:

```kotlin
sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    object Unauthenticated : AuthState()
    data class Error(val exception: Exception) : AuthState()
}
```

### User Data Model
```kotlin
data class User(
    val uid: String,
    val name: String,
    val email: String,
    val photoURL: String,
    val createdAt: Date?,
    val gdprConsent: Boolean,
    val consentedAt: Date?,
    val likedItemIds: List<String>,
    val dislikedItemIds: List<String>,
    val isAnonymous: Boolean
)
```

## Firestore Schema

User documents are stored in the `users` collection:

```json
{
  "uid": "firebase_uid",
  "name": "User Name",
  "email": "user@example.com",
  "photoURL": "https://...",
  "createdAt": "2024-01-12T10:30:00Z",
  "gdprConsent": true,
  "consentedAt": "2024-01-12T10:30:00Z",
  "likedItemIds": ["item1", "item2"],
  "dislikedItemIds": ["item3"]
}
```

## Firebase Configuration

Ensure your `google-services.json` is configured correctly:

1. Place `google-services.json` in `app/` directory (not in version control)
2. Add the Google Services plugin in `build.gradle`:
   ```kotlin
   plugins {
       id("com.google.gms.google-services")
   }
   ```
3. Add Firebase dependencies in `build.gradle`:
   ```kotlin
   implementation(libs.firebase.auth)
   implementation(libs.firebase.firestore)
   implementation(libs.firebase.storage)
   implementation(libs.firebase.functions)
   implementation(libs.play.services.auth) // For Google Sign-In
   ```

## Usage Examples

### In a Fragment (Fragment-based UI)

```kotlin
class LoginFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        authViewModel.authState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is AuthState.Loading -> showLoadingView()
                is AuthState.Authenticated -> navigateToHome()
                is AuthState.Unauthenticated -> showLoginForm()
                is AuthState.Error -> showError(state.exception.message)
            }
        }
    }
}
```

### Signup with Email/Password

```kotlin
authViewModel.signUp(
    email = "user@example.com",
    password = "securePassword123",
    name = "John Doe",
    hasConsent = true
)
```

### Login with Email/Password

```kotlin
authViewModel.login(
    email = "user@example.com",
    password = "securePassword123"
)
```

### Logout

```kotlin
authViewModel.signOut()
```

## Security Considerations

1. **Environment Variables**: Sensitive API keys are stored in `google-services.json`
2. **GDPR Compliance**: User consent is tracked in Firestore with timestamps
3. **Email Validation**: Prevents temporary/disposable email addresses
4. **Password Requirements**: Minimum 6 characters enforced
5. **Error Handling**: Sensitive error details not exposed to production users
6. **Graceful Degradation**: Auth continues even if Firestore enrichment fails

## Comparison with Web App

| Feature | Web App | Android |
|---------|---------|---------|
| Auth State | React Context + hooks | ViewModel + StateFlow |
| Firebase Config | `firebase.js` with env vars | `google-services.json` |
| Email Validation | `emailValidation.js` | `EmailValidator.kt` |
| Google Sign-In | `signInWithGoogle()` | `GoogleSignInHelper.kt` |
| User Creation | `createGoogleUser()` | `GoogleSignInHelper.signInWithGoogle()` |
| GDPR Tracking | Firestore document fields | Same Firestore schema |
| UI Components | React JSX | Jetpack Compose |
| Dependency Injection | Context API | Hilt DI |

## Testing

When implementing tests, consider:

1. **Mock Firebase Auth** for unit testing
2. **Use Firestore emulator** for integration tests
3. **Test email validation** separately
4. **Mock AuthViewModel** state flows in UI tests
5. **Test error scenarios** (network failures, invalid credentials)

## Common Issues & Solutions

### Google Sign-In Returns Empty idToken
- Ensure web client ID is configured in `GoogleSignInOptions`
- Verify `google-services.json` has correct OAuth configuration

### Firestore Permission Denied
- Check Firebase Security Rules in Console
- Ensure user is authenticated before Firestore operations

### Email Validation Too Strict
- Add custom domains to `LEGITIMATE_DOMAINS` in `EmailValidator.kt`
- Consider organization-specific email validation needs

## Future Enhancements

1. **Two-factor authentication (2FA)**
2. **Social auth providers** (Facebook, Apple)
3. **Phone authentication**
4. **Biometric authentication**
5. **Password reset flow**
6. **Email verification**
7. **Rate limiting** on login attempts

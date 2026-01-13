# Firebase Authentication System - Architecture Diagrams

## System Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        User Interface Layer                      │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │  LoginFragment   │  │LoginRequiredFrgmt│  │  More UIs... │  │
│  │  (Fragment)      │  │  (Fragment)      │  │  (Fragment)  │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────┬───────┘  │
│           │                     │                    │          │
│           └─────────────────────┴────────────────────┘          │
│                         │                                        │
│                         ▼                                        │
│            ┌────────────────────────┐                           │
│            │  AuthViewModel         │                           │
│            │  (State Management)    │                           │
│            │  - authState: LiveData │                           │
│            │  - login()             │                           │
│            │  - signUp()            │                           │
│            │  - signInWithGoogle()  │                           │
│            │  - signOut()           │                           │
│            └────────────┬───────────┘                           │
│                         │                                        │
└─────────────────────────┼────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Domain Layer (Use Cases)                      │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────┐  │
│  │ SignUpUseCase    │  │GoogleSignInUseCase│ │EnrichUserUC  │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────┬───────┘  │
│           │                     │                    │          │
│           └─────────────────────┴────────────────────┘          │
│                         │                                        │
│                         ▼                                        │
│            ┌────────────────────────┐                           │
│            │  AuthRepository        │                           │
│            │  (Interface)           │                           │
│            │  - login()             │                           │
│            │  - signUp()            │                           │
│            │  - signInWithGoogle()  │                           │
│            │  - enrichUserData()    │                           │
│            └────────────┬───────────┘                           │
│                         │                                        │
└─────────────────────────┼────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Data Layer (Repositories)                     │
│            ┌────────────────────────────────┐                   │
│            │  AuthRepositoryImpl             │                   │
│            │  - Implements AuthRepository   │                   │
│            │  - Coordinates Firebase calls  │                   │
│            └────┬──────────┬──────────┬─────┘                   │
│                 │          │          │                         │
│       ┌─────────▼──┐  ┌────▼────┐   │                          │
│       │  Firebase  │  │ Google  │   │                          │
│       │   Auth     │  │ Sign-In │   │                          │
│       └────────────┘  └─────────┘   │                          │
│                                     │                          │
│                            ┌────────▼────────┐                 │
│                            │  Firestore      │                 │
│                            │  (User Data)    │                 │
│                            └─────────────────┘                 │
│                                                                │
│  ┌────────────────────────────────────────────────────────┐  │
│  │  EmailValidator.kt - Email validation utility          │  │
│  │  FirebaseInitializer.kt - Firebase setup              │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

## Authentication Flow Diagrams

### Email/Password Signup Flow

```
User fills signup form
        │
        ▼
[Name, Email, Password, GDPR Consent]
        │
        ▼
EmailValidator.validateEmail()
        │
    ┌───┴───┐
    │       │
Invalid   Valid
    │       │
    ▼       ▼
Show    authViewModel.signUp()
Error       │
            ▼
        AuthRepositoryImpl.signUp()
            │
            ▼
        Firebase Auth: createUserWithEmailAndPassword()
            │
        ┌───┴───┐
        │       │
      Fail    Success
        │       │
        ▼       ▼
    Error   updateUserProfile()
    State       │
                ▼
            Firestore: users/{uid}.set()
                │
                ▼
            AuthViewModel authStateListener
                │
                ▼
            enrichUserWithFirestoreData()
                │
                ▼
            AuthState.Authenticated
                │
                ▼
            Navigate to Home
```

### Google Sign-In Flow

```
User taps "Sign in with Google"
        │
        ▼
GoogleSignInHelper.googleSignInClient.signInIntent
        │
        ▼
User selects Google account
        │
        ▼
OnActivityResult(result) → Get idToken
        │
        ▼
authViewModel.signInWithGoogle(idToken)
        │
        ▼
AuthRepositoryImpl.signInWithGoogle()
        │
        ▼
GoogleSignInHelper.signInWithGoogle()
        │
        ▼
Firebase Auth: signInWithCredential()
        │
    ┌───┴───────────────┐
    │                   │
New User         Existing User
    │                   │
    ▼                   ▼
createGoogleUserDoc()  updateExistingUserIfNeeded()
    │                   │
    └───────────┬───────┘
                ▼
        Firestore: users/{uid}.set()
                │
                ▼
        AuthViewModel authStateListener
                │
                ▼
        enrichUserWithFirestoreData()
                │
                ▼
        AuthState.Authenticated
                │
                ▼
        Navigate to Home
```

### Protected Route Flow

```
User navigates to protected route
        │
        ▼
Check authState
        │
    ┌───┴──────┬──────────┬─────────┐
    │          │          │         │
Loading   Authenticated  Unauthent  Error
    │          │          │         │
    ▼          ▼          ▼         ▼
Loading   Show Route   LoginRequired Error
Screen    Content      Screen       Screen
```

## State Management Flow

```
┌─────────────────────────────────────────────────────┐
│         AuthState (Sealed Class)                    │
├─────────────────────────────────────────────────────┤
│ ┌──────────────────────────────────────────────┐   │
│ │ Loading: Indicates auth operation in progress│   │
│ └──────────────────────────────────────────────┘   │
│ ┌──────────────────────────────────────────────┐   │
│ │ Authenticated(user: User)                     │   │
│ │ - Contains enriched user data from Firestore  │   │
│ │ - Includes GDPR consent, liked/disliked items│   │
│ └──────────────────────────────────────────────┘   │
│ ┌──────────────────────────────────────────────┐   │
│ │ Unauthenticated: No current user              │   │
│ └──────────────────────────────────────────────┘   │
│ ┌──────────────────────────────────────────────┐   │
│ │ Error(exception: Exception)                   │   │
│ │ - Contains error details for UI display       │   │
│ └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘

State Transitions:
┌────────────────────────────────────────────────────┐
│           Loading ─────────┐                       │
│            ▲ ▲ ▲           │                       │
│            │ │ │           ├──► Authenticated      │
│            │ │ └──────────►├──► Unauthenticated    │
│            │ └────────────►├──► Error              │
│            │               │                       │
│            └───────────────┘                       │
│                                                    │
│ Initial: Loading                                  │
│ After auth check: Authenticated or Unauthenticated│
│ On error: Error                                   │
│ On logout: Unauthenticated                        │
│ On login: Authenticated                           │
└────────────────────────────────────────────────────┘
```

## Dependency Injection Structure

```
┌────────────────────────────────────────────────┐
│        Hilt DI Module (AuthModule)             │
├────────────────────────────────────────────────┤
│                                                │
│  @Singleton                                   │
│  ┌──────────────────────────────────────┐    │
│  │ provideFirebaseAuth()                │    │
│  │ → FirebaseInitializer.auth           │    │
│  └──────────────────────────────────────┘    │
│                                                │
│  @Singleton                                   │
│  ┌──────────────────────────────────────┐    │
│  │ provideFirebaseFirestore()           │    │
│  │ → FirebaseInitializer.firestore      │    │
│  └──────────────────────────────────────┘    │
│                                                │
│  @Singleton                                   │
│  ┌──────────────────────────────────────┐    │
│  │ provideGoogleSignInHelper()          │    │
│  │ → GoogleSignInHelper(context)        │    │
│  └──────────────────────────────────────┘    │
│                                                │
│  @Singleton                                   │
│  ┌──────────────────────────────────────┐    │
│  │ provideAuthRepository()              │    │
│  │ → AuthRepositoryImpl(                 │    │
│  │     auth,                            │    │
│  │     firestore,                       │    │
│  │     googleSignInHelper               │    │
│  │   )                                  │    │
│  └──────────────────────────────────────┘    │
│                                                │
└────────────────────────────────────────────────┘
         │
         │ (Injection)
         ▼
    AuthViewModel
    └─ consumes:
       ├─ FirebaseAuth
       ├─ AuthRepository
       └─ EnrichUserUseCase
```

## Firestore Data Structure

```
Firebase Project
│
├── Authentication
│   ├── Email/Password Provider
│   └── Google Sign-In Provider
│
└── Firestore Database
    │
    └── users (Collection)
        │
        └── {userId} (Document)
            │
            ├── uid: string
            ├── name: string
            ├── email: string
            ├── photoURL: string
            ├── createdAt: timestamp
            ├── gdprConsent: boolean
            ├── consentedAt: timestamp
            ├── likedItemIds: array
            └── dislikedItemIds: array
```

## Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Fragments)                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Fragment-based Screens                              │  │
│  │ ├─ LoginFragment                                     │  │
│  │ ├─ LoginRequiredFragment                             │  │
│  │ └─ Other Fragments (Chat, Home, etc)                 │  │
│  └──────────────────────────────────────────────────────┘  │
│                         │                                   │
│  getViewModel()◄────────┘                                   │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ AuthViewModel                                        │  │
│  │ ├─ StateFlow<AuthState>                             │  │
│  │ ├─ login()                                           │  │
│  │ ├─ signUp()                                          │  │
│  │ ├─ signInWithGoogle()                               │  │
│  │ └─ signOut()                                         │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                 Domain Layer                                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Use Cases                                            │  │
│  │ ├─ SignUpUseCase                                     │  │
│  │ ├─ GoogleSignInUseCase                               │  │
│  │ └─ EnrichUserUseCase                                 │  │
│  └──────────────────────────────────────────────────────┘  │
│                         │                                   │
│                         ▼                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ AuthRepository (Interface)                           │  │
│  │ ├─ login()                                           │  │
│  │ ├─ signUp()                                          │  │
│  │ ├─ signInWithGoogle()                               │  │
│  │ └─ enrichUserWithFirestoreData()                     │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ AuthRepositoryImpl                                   │  │
│  │ ├─ Uses GoogleSignInHelper                          │  │
│  │ ├─ Calls Firebase Auth                              │  │
│  │ └─ Reads/Writes Firestore                           │  │
│  └──────────────────────────────────────────────────────┘  │
│                    │        │        │                     │
│         ┌──────────┘        │        └──────────┐          │
│         │                   │                   │          │
│         ▼                   ▼                   ▼          │
│  ┌────────────────┐  ┌─────────┐  ┌──────────────┐        │
│  │ Firebase Auth  │  │Firestore│  │ GoogleSignIn │        │
│  │                │  │ (Users) │  │   Helper    │        │
│  └────────────────┘  └─────────┘  └──────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

## Comparison: Web vs Android Architecture

```
Web App (React)              Android (Kotlin)
─────────────────────────────────────────────────────
firebase.js              →    FirebaseInitializer.kt
                             GoogleSignInHelper.kt

AuthContext              →    AuthViewModel
useAuth() hook           →    hiltViewModel()
useState()               →    mutableStateOf()
useEffect()              →    LaunchedEffect()

onAuthStateChanged()     →    authStateListener

Context.Provider         →    Hilt DI Module
PropTypes                →    Data classes (sealed class)

JSX Components           →    Jetpack Compose
useState hooks           →    remember { mutableStateOf() }
useNavigate()            →    NavController

emailValidation.js       →    EmailValidator.kt
```

---

These diagrams provide a visual understanding of:
- Overall system architecture and layering
- Authentication flow paths
- State management transitions
- Dependency injection structure
- Firestore data organization
- Component interactions
- Web-to-Android pattern mapping

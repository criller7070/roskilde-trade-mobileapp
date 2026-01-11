# Android User State Management - Auth System

This implementation mirrors your React webapp's `AuthContext` pattern, adapted for Kotlin/Android using modern best practices following **Clean Architecture**.

## Architecture Overview

The system follows the **Clean Architecture pattern** with MVVM, dependency injection (Hilt), and coroutines.

### Components

#### Domain Layer (`feature/auth/domain/`)

**User** (`User.kt`)
Data class representing a user in the app:
```kotlin
data class User(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoURL: String?,
    val isAnonymous: Boolean
)
```

**AuthState** (`AuthState.kt`)
Sealed class representing the authentication state:
```kotlin
sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data object Unauthenticated : AuthState()
    data class Error(val exception: Exception) : AuthState()
}
```

**AuthRepository** (interface in `AuthRepository.kt`)
Defines the contract for authentication operations:
```kotlin
interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun enrichUserWithFirestoreData(baseUser: User): User
}
```

**EnrichUserUseCase** (`EnrichUserUseCase.kt`)
Use case for enriching user data with Firestore information.

#### Data Layer (`feature/auth/data/`)

**FirebaseAuthRepository** (`AuthRepositoryImpl.kt`)
Implementation of AuthRepository:
- Handles Firebase Auth login
- Fetches additional user data from Firestore
- Enriches the base user object with `displayName` and `photoURL` from Firestore
- Gracefully handles Firestore errors without breaking authentication

#### Presentation Layer (`feature/auth/presentation/`)

**AuthViewModel** (`AuthViewModel.kt`)
Main ViewModel managing auth state:
- Listens to Firebase Auth state changes via `addAuthStateListener`
- Automatically enriches user data from Firestore
- Exposes `authState: StateFlow<AuthState>` for UI observation
- Uses coroutines to handle async Firestore operations
- Automatically cleans up listeners when ViewModel is destroyed

**AuthObserverExtensions** (`AuthObserverExtensions.kt`)
Helper extension functions for Fragments to observe auth state in a lifecycle-aware manner.

## Usage in Your Fragments

### Basic Setup

```kotlin
class HomeFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        observeAuthState(authViewModel) { state ->
            when (state) {
                is AuthState.Loading -> showLoading()
                is AuthState.Authenticated -> setupUserUI(state.user)
                is AuthState.Unauthenticated -> navigateToLogin()
                is AuthState.Error -> showErrorMessage(state.exception)
            }
        }
    }

    private fun setupUserUI(user: User) {
        binding.userName.text = user.displayName ?: user.email
        // Setup rest of UI with user data
    }
}
```

### Sign Out

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authViewModel: AuthViewModel
) : ViewModel() {
    
    fun signOut() {
        authViewModel.signOut()
    }
}
```

### Advanced: Accessing User Data Directly

```kotlin
class UserProfileFragment : Fragment() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        observeAuthState(authViewModel) { state ->
            if (state is AuthState.Authenticated) {
                val user = state.user
                binding.email.text = user.email
                binding.displayName.text = user.displayName
                // Load photo if available
                if (user.photoURL != null) {
                    loadProfilePhoto(user.photoURL)
                }
            }
        }
    }
}
```

## How It Works

### 1. **Initialization** (when ViewModel is created)
```
AuthViewModel.init()
  ↓
observeAuthState() - Sets up Firebase Auth listener
  ↓
Initial state: AuthState.Loading
```

### 2. **User Login**
```
FirebaseAuth.onAuthStateChanged(currentUser != null)
  ↓
Create baseUser from Firebase Auth
  ↓
enrichUserAsync(baseUser)
  ↓
viewModelScope.launch {
    enrichUserUseCase(baseUser)
      ↓
    FirebaseAuthRepository.enrichUserWithFirestoreData()
      → Fetch from Firestore /users/{uid}
      → Merge displayName and photoURL
      → Handle errors gracefully
  ↓
Emit AuthState.Authenticated(enrichedUser)
```

### 3. **User Logout**
```
signOut()
  ↓
FirebaseAuth.signOut()
  ↓
Firebase auth listener fires with currentUser == null
  ↓
Emit AuthState.Unauthenticated
```

## Error Handling

The system gracefully handles errors at multiple levels:

- **Firestore errors**: If Firestore fetch fails, the user remains authenticated with base data
- **Auth errors**: Any unexpected errors emit `AuthState.Error`
- **Lifecycle-aware observation**: Automatically stops observing when Fragment stops to prevent leaks

All errors are logged to Android logcat with the `TAG = "AuthViewModel"` or `TAG = "FirebaseAuthRepo"`.

## Firestore Data Structure

This implementation expects a Firestore structure like:

```
/users/{uid}
  ├── name: String
  └── photoURL: String
```

The system enriches the user object by merging Firebase Auth fields with Firestore data:
- Uses Firebase Auth's `displayName` if available
- Falls back to Firestore's `name` field
- Same pattern for `photoURL`

## Dependency Injection

The auth module is automatically provided by Hilt:

```kotlin
// In AuthModule (if custom configuration needed)
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AuthRepository {
        return FirebaseAuthRepository(auth, firestore)
    }
}
```

Since you already have AuthModule in your `di/` folder, Hilt will automatically provide these dependencies.

## Clean Architecture Benefits

| Benefit | Implementation |
|---------|-----------------|
| Testability | Repository interface allows mocking |
| Reusability | Use cases can be used across features |
| Maintainability | Clear separation of concerns |
| Scalability | Easy to add new auth features |
| Independence | Domain layer has no Android/Firebase dependencies |

## Testing

Example unit test:

```kotlin
@RunWith(AndroidJUnit4::class)
class AuthViewModelTest {
    private lateinit var viewModel: AuthViewModel
    private val mockFirebaseAuth = mockk<FirebaseAuth>()
    private val mockEnrichUserUseCase = mockk<EnrichUserUseCase>()

    @Before
    fun setup() {
        viewModel = AuthViewModel(mockFirebaseAuth, mockEnrichUserUseCase)
    }

    @Test
    fun testAuthStateChangeToAuthenticated() {
        // Setup mock to return authenticated state
        // Verify authState emits Authenticated when user logs in
    }
}
```

## Next Steps

1. **Test the auth system** with your Firebase project
2. **Integrate with existing LoginFragment** 
3. **Add state-based navigation** in MainActivity based on AuthState
4. **Customize Firestore enrichment** with additional user fields as needed


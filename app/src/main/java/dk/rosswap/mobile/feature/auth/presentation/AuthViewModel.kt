package dk.rosswap.mobile.feature.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.common.User
import dk.rosswap.mobile.feature.auth.domain.EnrichUserUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing authentication state across the app.
 * Mirrors the React AuthContext pattern using Kotlin StateFlow and Firebase Auth.
 *
 * Usage in a Fragment:
 * ```kotlin
 * val authViewModel = viewModels<AuthViewModel>()
 * observeAuthState(authViewModel) { state ->
 *     when (state) {
 *         is AuthState.Loading -> showLoadingScreen()
 *         is AuthState.Authenticated -> showMainApp(state.user)
 *         is AuthState.Unauthenticated -> showLoginScreen()
 *         is AuthState.Error -> showError(state.exception)
 *     }
 * }
 * ```
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val enrichUserUseCase: EnrichUserUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        observeAuthState()
    }

    /**
     * Sets up a listener for Firebase auth state changes.
     * When the user logs in/out, this automatically updates the authState.
     * Mirrors the React onAuthStateChanged() listener.
     */
    private fun observeAuthState() {
        firebaseAuth.addAuthStateListener { auth ->
            val firebaseUser = auth.currentUser

            if (firebaseUser == null) {
                /* User is not authenticated */
                _authState.value = AuthState.Unauthenticated
                return@addAuthStateListener
            }

            /* User exists, enrich with Firestore data */
            val baseUser = User(
                uid = firebaseUser.uid,
                email = firebaseUser.email ?: "",
                name = firebaseUser.displayName ?: "",
                photoURL = firebaseUser.photoURL?.toString() ?: "",
                isAnonymous = firebaseUser.isAnonymous
            )

            /* Fetch and enrich user data in a coroutine */
            enrichUserAsync(baseUser)
        }
    }

    /**
     * Launches an async coroutine to enrich user data from Firestore.
     * Uses viewModelScope to automatically cancel when ViewModel is cleared.
     */
    private fun enrichUserAsync(baseUser: User) {
        viewModelScope.launch {
            try {
                val enrichedUser = enrichUserUseCase(baseUser)
                _authState.value = AuthState.Authenticated(enrichedUser)
            } catch (e: Exception) {
                Log.e(TAG, "Error enriching user", e)
                _authState.value = AuthState.Error(e)
            }
        }
    }

    /**
     * Signs out the current user.
     */
    fun signOut() {
        try {
            firebaseAuth.signOut()
            _authState.value = AuthState.Unauthenticated
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            _authState.value = AuthState.Error(e)
        }
    }
}

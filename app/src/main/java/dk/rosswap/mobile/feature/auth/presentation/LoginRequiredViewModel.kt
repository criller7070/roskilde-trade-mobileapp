package dk.rosswap.mobile.feature.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginRequiredViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository
) : ViewModel() {
    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser

            if (firebaseUser == null) {
                /* User is not authenticated */
                _authState.value = AuthState.Unauthenticated
                return@AuthStateListener
            }

            /* User exists, enrich with Firestore data */
            val baseUser = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                photoURL = firebaseUser.photoUrl?.toString() ?: "",
                createdAt = null,
                gdprConsent = false,
                consentedAt = null,
                likedItemIds = emptyList(),
                dislikedItemIds = emptyList(),
                emailVerified = firebaseUser.isEmailVerified,
                isAnonymous = firebaseUser.isAnonymous
            )

            /* Fetch and enrich user data in a coroutine */
            enrichUserAsync(baseUser)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    private var enrichmentJob: Job? = null

    private fun enrichUserAsync(baseUser: User) {
        // Cancel any in-flight enrichment job to prevent race conditions
        enrichmentJob?.cancel()

        enrichmentJob = viewModelScope.launch {
            try {
                val enrichedUser = authRepository.enrichUserWithFirestoreData(baseUser)
                _authState.value = AuthState.Authenticated(enrichedUser)
            } catch (e: Exception) {
                Log.e(TAG, "Error enriching user", e)
                _authState.value = AuthState.Error(e)
            }
        }
    }

    fun signOut() {
        try {
            // Cancel any in-flight enrichment job to prevent race conditions during sign out
            enrichmentJob?.cancel()
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            _authState.value = AuthState.Error(e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::authStateListener.isInitialized) {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
}

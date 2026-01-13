package dk.rosswap.mobile.feature.auth.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.common.User
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import dk.rosswap.mobile.feature.auth.domain.EnrichUserUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel managing authentication state across the app.
 * Observes Firebase auth state changes and enriches user data from Firestore.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository,
    private val enrichUserUseCase: EnrichUserUseCase
) : ViewModel() {
    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _authState = MutableLiveData<AuthState>(AuthState.Loading)
    val authState: LiveData<AuthState> = _authState

    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser

            if (firebaseUser == null) {
                _authState.postValue(AuthState.Unauthenticated)
                return@AuthStateListener
            }

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
                isAnonymous = firebaseUser.isAnonymous
            )

            enrichUserAsync(baseUser)
        }
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    private var enrichmentJob: Job? = null

    private fun enrichUserAsync(baseUser: User) {
        enrichmentJob?.cancel()
        
        enrichmentJob = viewModelScope.launch {
            try {
                val enrichedUser = enrichUserUseCase(baseUser)
                _authState.postValue(AuthState.Authenticated(enrichedUser))
            } catch (e: Exception) {
                Log.e(TAG, "Error enriching user", e)
                _authState.postValue(AuthState.Error(e))
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.postValue(AuthState.Loading)
                val result = authRepository.login(email, password)
                result.onFailure { error ->
                    Log.e(TAG, "Login failed: ${error.message}", error)
                    _authState.postValue(AuthState.Error(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login exception: ${e.message}", e)
                _authState.postValue(AuthState.Error(e))
            }
        }
    }

    fun signUp(email: String, password: String, name: String, hasConsent: Boolean) {
        viewModelScope.launch {
            try {
                _authState.postValue(AuthState.Loading)
                val result = authRepository.signUp(email, password, name, hasConsent)
                result.onFailure { error ->
                    Log.e(TAG, "Sign-up failed: ${error.message}", error)
                    _authState.postValue(AuthState.Error(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Sign-up exception: ${e.message}", e)
                _authState.postValue(AuthState.Error(e))
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.postValue(AuthState.Loading)
                val result = authRepository.signInWithGoogle(idToken)
                result.onFailure { error ->
                    Log.e(TAG, "Google sign-in failed: ${error.message}", error)
                    _authState.postValue(AuthState.Error(error))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google sign-in exception: ${e.message}", e)
                _authState.postValue(AuthState.Error(e))
            }
        }
    }

    fun signOut() {
        try {
            enrichmentJob?.cancel()
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            _authState.postValue(AuthState.Error(e))
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::authStateListener.isInitialized) {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
}


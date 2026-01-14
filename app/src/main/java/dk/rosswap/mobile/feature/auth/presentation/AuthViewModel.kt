package dk.rosswap.mobile.feature.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.common.AuthStateImpl
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.asLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val authRepository: AuthRepository
) : ViewModel() {
    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _authStateImpl = MutableStateFlow<AuthStateImpl>(AuthStateImpl.Loading)
    val authState: StateFlow<AuthState> = _authStateImpl.asStateFlow()

    // Backwards compatible accessor for code that used the concrete impl name
    val authStateImpl: StateFlow<AuthStateImpl> = _authStateImpl.asStateFlow()

    // LiveData adapter for components that still observe LiveData
    val authStateImplLiveData: LiveData<AuthStateImpl> = _authStateImpl.asLiveData()

    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser

            if (firebaseUser == null) {
                /* User is not authenticated */
                _authStateImpl.value = AuthStateImpl.Unauthenticated
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
        enrichmentJob?.cancel()

        enrichmentJob = viewModelScope.launch {
            try {
                val enrichedUser = authRepository.enrichUserWithFirestoreData(baseUser)
                _authStateImpl.value = AuthStateImpl.Authenticated(enrichedUser)
            } catch (e: Exception) {
                Log.e(TAG, "Error enriching user", e)
                _authStateImpl.value = AuthStateImpl.Error(e)
            }
        }
    }

    // ---- Backwards-compatible LiveData APIs for login screen ---------------------------------
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginResult = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> = _loginResult

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val result = authRepository.login(email, password)
                _loginResult.postValue(result)
                result.exceptionOrNull()?.let { ex ->
                    _authStateImpl.value = AuthStateImpl.Error(ex)
                }
            } catch (e: Exception) {
                _loginResult.postValue(Result.failure(e))
                _authStateImpl.value = AuthStateImpl.Error(e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val result = authRepository.signInWithGoogle(idToken)
                result.exceptionOrNull()?.let { ex -> _authStateImpl.value = AuthStateImpl.Error(ex) }
            } catch (e: Exception) {
                _authStateImpl.value = AuthStateImpl.Error(e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun signOut() {
        try {
            enrichmentJob?.cancel()
            firebaseAuth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            _authStateImpl.value = AuthStateImpl.Error(e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (::authStateListener.isInitialized) {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
}

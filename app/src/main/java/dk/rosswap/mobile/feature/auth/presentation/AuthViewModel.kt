package dk.rosswap.mobile.feature.auth.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.lifecycle.asLiveData
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dk.rosswap.mobile.core.common.SessionManager

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository
) : ViewModel() {
    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val _authStateImpl = MutableStateFlow<AuthState>(AuthState.Loading)
    @Suppress("unused")
    val authState: StateFlow<AuthState> = _authStateImpl.asStateFlow()

    // Backwards compatible accessor for code that used the concrete impl name
    val authStateImpl: StateFlow<AuthState> = _authStateImpl.asStateFlow()

    // LiveData adapter for components that still observe LiveData
    val authStateImplLiveData: LiveData<AuthState> = _authStateImpl.asLiveData()
    // Backwards-compatible name used across the app
    val authStateLiveData: LiveData<AuthState> = authStateImplLiveData

    init {
        // Mirror the SessionManager's authState into the ViewModel's flow for compatibility
        viewModelScope.launch {
            sessionManager.authState.collect { state ->
                // Mirror the SessionManager's AuthState directly
                _authStateImpl.value = state
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
                    _authStateImpl.value = AuthState.Error(ex)
                }
            } catch (e: Exception) {
                _loginResult.postValue(Result.failure(e))
                _authStateImpl.value = AuthState.Error(e)
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
                result.exceptionOrNull()?.let { ex -> _authStateImpl.value = AuthState.Error(ex) }
            } catch (e: Exception) {
                _authStateImpl.value = AuthState.Error(e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // Backwards-compatible signUp delegate used by the UI
    fun signUp(email: String, password: String, name: String, hasConsent: Boolean) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val result = authRepository.signUp(email, password, name, hasConsent)
                _loginResult.postValue(result)
                result.exceptionOrNull()?.let { ex -> _authStateImpl.value = AuthState.Error(ex) }
            } catch (e: Exception) {
                _loginResult.postValue(Result.failure(e))
                _authStateImpl.value = AuthState.Error(e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    @Suppress("unused")
    fun signOut() {
        try {
            // Delegate sign out to SessionManager
            sessionManager.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error signing out", e)
            _authStateImpl.value = AuthState.Error(e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // nothing to cleanup - SessionManager is app-scoped
    }
}

package dk.rosswap.mobile.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class LoginEvent {
    object NavigateToHome : LoginEvent()
    data class ShowError(val message: String) : LoginEvent()
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>()
    val events: SharedFlow<LoginEvent> = _events

    // login button
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = authRepository.login(email, password)
                if (result.isSuccess) {
                    _events.emit(LoginEvent.NavigateToHome)
                } else {
                    _events.emit(LoginEvent.ShowError(result.exceptionOrNull()?.message ?: "Login failed"))
                }
            } catch (e: Exception) {
                _events.emit(LoginEvent.ShowError(e.message ?: "Login failed"))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    // sign in with google button
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val result = authRepository.signInWithGoogle(idToken)
                if (result.isSuccess) {
                    _events.emit(LoginEvent.NavigateToHome)
                } else {
                    _events.emit(LoginEvent.ShowError(result.exceptionOrNull()?.message ?: "Google sign-in failed"))
                }
            } catch (e: Exception) {
                _events.emit(LoginEvent.ShowError(e.message ?: "Google sign-in failed"))
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
}

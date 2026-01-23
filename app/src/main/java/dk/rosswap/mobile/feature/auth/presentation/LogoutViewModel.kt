package dk.rosswap.mobile.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// its arguable whether a ViewModel is even needed here. It works out to be a very
// simple wrapper with lots of checks, but now that it's done it might as well say

sealed class LogoutEvent {
    object SignedOut : LogoutEvent()
    data class Error(val message: String) : LogoutEvent()
}

@HiltViewModel
class LogoutViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _events = MutableSharedFlow<LogoutEvent>()
    val events: SharedFlow<LogoutEvent> = _events

    fun signOut() {
        viewModelScope.launch {
            try {
                sessionManager.signOut()
                _events.emit(LogoutEvent.SignedOut)
            } catch (e: Exception) {
                _events.emit(LogoutEvent.Error(e.message ?: "Sign out failed"))
            }
        }
    }
}

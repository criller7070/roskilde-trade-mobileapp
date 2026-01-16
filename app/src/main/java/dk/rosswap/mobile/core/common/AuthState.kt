package dk.rosswap.mobile.core.common

import dk.rosswap.mobile.core.model.User
import kotlinx.coroutines.flow.StateFlow

sealed class AuthState {
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    object Unauthenticated : AuthState()

    data class Error(val exception: Throwable) : AuthState()
}

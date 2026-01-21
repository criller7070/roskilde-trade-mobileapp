package dk.rosswap.mobile.core.common

import dk.rosswap.mobile.core.model.User

sealed class AuthState {
    data object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data object Unauthenticated : AuthState()

    data class Error(val exception: Throwable) : AuthState()
}

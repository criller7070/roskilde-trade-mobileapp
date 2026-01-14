package dk.rosswap.mobile.core.common

import dk.rosswap.mobile.core.model.User

sealed class AuthStateImpl : AuthState {

    data object Loading : AuthStateImpl()

    data class Authenticated(val user: User) : AuthStateImpl()

    data object Unauthenticated : AuthStateImpl()

    data class Error(val exception: Throwable) : AuthStateImpl()
}
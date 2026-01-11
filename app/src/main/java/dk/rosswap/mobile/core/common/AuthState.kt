package dk.rosswap.mobile.core.common

/**
 * Sealed class representing the authentication state of the app.
 * Mirrors the React pattern of user + loading state.
 */
sealed class AuthState {
    /**
     * Auth is currently initializing.
     */
    data object Loading : AuthState()

    /**
     * User is authenticated. Contains the user data.
     */
    data class Authenticated(val user: User) : AuthState()

    /**
     * User is not authenticated.
     */
    data object Unauthenticated : AuthState()

    /**
     * An error occurred during authentication.
     */
    data class Error(val exception: Exception) : AuthState()
}
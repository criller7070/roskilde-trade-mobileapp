package dk.rosswap.mobile.core.common

import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.StateFlow

// interface for session manager to centralize auth state

interface SessionManager {
    val authState: StateFlow<AuthState>

    fun currentUserId(): String?

    fun signOut()

    suspend fun updateProfile(profileUpdates: UserProfileChangeRequest): Result<Unit>

    suspend fun deleteAccount(): Result<Unit>
}
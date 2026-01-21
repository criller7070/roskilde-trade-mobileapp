package dk.rosswap.mobile.core.common

import android.util.Log
import com.google.firebase.auth.UserProfileChangeRequest
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import dk.rosswap.mobile.core.mappers.UserMapper
import dk.rosswap.mobile.core.data.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Suppress("unused")
class SessionManagerImpl(
    private val authRepo: AuthRepository
) : SessionManager {

    companion object {
        private const val TAG = "SessionManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        val current = authRepo.currentFirebaseUser()
        handleFirebaseUserChange(current)

        scope.launch {
            authRepo.authUserFlow().collect { firebaseUser ->
                handleFirebaseUserChange(firebaseUser)
            }
        }
    }

    private fun handleFirebaseUserChange(firebaseUser: com.google.firebase.auth.FirebaseUser?) {
        if (firebaseUser == null) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        scope.launch {
            try {
                val baseUser = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    photoURL = firebaseUser.photoUrl?.toString() ?: "",
                    // other fields left as defaults
                )

                val enriched = authRepo.enrichUserWithFirestoreData(baseUser)
                _authState.value = AuthState.Authenticated(enriched)
            } catch (e: Exception) {
                Log.w(TAG, "Failed enriching user", e)
                _authState.value = AuthState.Error(e)
            }
        }
    }

    @Suppress("unused")
    override fun currentUserId(): String? = (authState.value as? AuthState.Authenticated)?.user?.uid

    @Suppress("unused")
    override fun signOut() {
        scope.launch {
            val res = authRepo.signOut()
            if (res.isSuccess) {
                _authState.value = AuthState.Unauthenticated
            } else {
                Log.w(TAG, "signOut failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    override suspend fun updateProfile(profileUpdates: UserProfileChangeRequest): Result<Unit> {
        val result = authRepo.updateProfile(profileUpdates)
        if (result.isSuccess) {
            val firebaseUser = authRepo.currentFirebaseUser() ?: return Result.failure(IllegalStateException("Not logged in"))
            val base = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                photoURL = firebaseUser.photoUrl?.toString() ?: ""
            )
            val enriched = authRepo.enrichUserWithFirestoreData(base)
            _authState.value = AuthState.Authenticated(enriched)
        }
        return result
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val result = authRepo.deleteCurrentUser()
        if (result.isSuccess) {
            _authState.value = AuthState.Unauthenticated
        }
        return result
    }
}
package dk.rosswap.mobile.core.common

import android.util.Log
import com.google.firebase.auth.UserProfileChangeRequest
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Session manager to centralize auth (logged in/out) states with associated methods
// we might consider adding a SessionModule next time

class SessionManagerImpl(
    private val authRepo: AuthRepository
) : SessionManager {

    companion object {
        private const val TAG = "SessionManager"
    }

    // scope is a bit confusing but it's just coroutine/async code in Kotlin
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // use AuthState.kt container in /common
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // initialize auth state to whatever state they are currently in
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
        // update auth state
        if (firebaseUser == null) {
            _authState.value = AuthState.Unauthenticated
            return
        }

        // enrich user with Firestore data
        scope.launch {
            try {
                val baseUser = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "",
                    email = firebaseUser.email ?: "",
                    photoURL = firebaseUser.photoUrl?.toString() ?: "",
                    // other fields left as defaults
                )

                // send over enriched user
                val enriched = authRepo.enrichUserWithFirestoreData(baseUser)
                _authState.value = AuthState.Authenticated(enriched)
            } catch (e: Exception) {
                Log.w(TAG, "Failed enriching user", e)
                _authState.value = AuthState.Error(e)
            }
        }
    }

    override fun currentUserId(): String? = (authState.value as? AuthState.Authenticated)?.user?.uid

    override fun signOut() {
        scope.launch {
            // use auth repo to sign out in repo
            val res = authRepo.signOut()
            if (res.isSuccess) {
                _authState.value = AuthState.Unauthenticated
            } else {
                Log.w(TAG, "signOut failed: ${res.exceptionOrNull()?.message}")
            }
        }
    }

    override suspend fun updateProfile(profileUpdates: UserProfileChangeRequest): Result<Unit> {
        // again use auth repo to update profile with profile updates if any
        val result = authRepo.updateProfile(profileUpdates)
        if (result.isSuccess) {
            val firebaseUser = authRepo.currentFirebaseUser() ?: return Result.failure(IllegalStateException("Not logged in"))
            val base = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                photoURL = firebaseUser.photoUrl?.toString() ?: ""
                // other fields left as defaults
            )
            val enriched = authRepo.enrichUserWithFirestoreData(base)
            _authState.value = AuthState.Authenticated(enriched)
        }
        return result
    }

    override suspend fun deleteAccount(): Result<Unit> {
        // again again use auth repo to delete account
        val result = authRepo.deleteCurrentUser()
        if (result.isSuccess) {
            _authState.value = AuthState.Unauthenticated
        }
        return result
    }
    // honestly next time we make an app we should consider what relation /auth feature
    // should relate to /common's session manager, because these just seem like wrappers
}
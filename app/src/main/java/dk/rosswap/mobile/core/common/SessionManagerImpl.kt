package dk.rosswap.mobile.core.common

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.data.UserDto
import dk.rosswap.mobile.core.mappers.UserMapper
import dk.rosswap.mobile.core.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Suppress("unused")
class SessionManagerImpl(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : SessionManager {

    companion object {
        private const val TAG = "SessionManager"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        val current = auth.currentUser
        handleFirebaseUserChange(current)

        auth.addAuthStateListener { firebaseAuth ->
            handleFirebaseUserChange(firebaseAuth.currentUser)
        }
    }

    private fun handleFirebaseUserChange(firebaseUser: FirebaseUser?) {
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

                val enriched = enrichUserWithFirestoreData(baseUser)
                _authState.value = AuthState.Authenticated(enriched)
            } catch (e: Exception) {
                Log.w(TAG, "Failed enriching user", e)
                _authState.value = AuthState.Error(e)
            }
        }
    }

    private suspend fun enrichUserWithFirestoreData(baseUser: User): User {
        return try {
            val snap = firestore.collection("users")
                .document(baseUser.uid)
                .get()
                .await()

            if (snap.exists()) {
                val data = snap.data ?: emptyMap<String, Any?>()
                val coreDto = UserDto(
                    uid = (data["uid"] as? String) ?: snap.id,
                    name = data["name"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    photoURL = data["photoURL"] as? String ?: "",
                    createdAt = data["createdAt"] as? Timestamp,
                    gdprConsent = data["gdprConsent"] as? Boolean ?: false,
                    consentedAt = data["consentedAt"] as? Timestamp,
                    likedItemIds = (data["likedItemIds"] as? List<*>)?.mapNotNull { it as? String }
                        ?: emptyList(),
                    dislikedItemIds = (data["dislikedItemIds"] as? List<*>)?.mapNotNull { it as? String }
                        ?: emptyList(),
                    emailVerified = data["emailVerified"] as? Boolean ?: false,
                    isAnonymous = data["isAnonymous"] as? Boolean ?: false
                )

                return UserMapper.fromDto(coreDto)
            } else {
                baseUser
            }
        } catch (error: Exception) {
            Log.w(TAG, "Enrichment error for user ${baseUser.uid}: ${error.message}")
            baseUser
        }
    }

    @Suppress("unused")
    override fun currentUserId(): String? = (authState.value as? AuthState.Authenticated)?.user?.uid

    @Suppress("unused")
    override fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    override suspend fun updateProfile(profileUpdates: UserProfileChangeRequest): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            user.updateProfile(profileUpdates).await()
            val base = User(
                uid = user.uid,
                name = user.displayName ?: "",
                email = user.email ?: "",
                photoURL = user.photoUrl?.toString() ?: ""
            )
            val enriched = enrichUserWithFirestoreData(base)
            _authState.value = AuthState.Authenticated(enriched)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            user.delete().await()
            _authState.value = AuthState.Unauthenticated
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
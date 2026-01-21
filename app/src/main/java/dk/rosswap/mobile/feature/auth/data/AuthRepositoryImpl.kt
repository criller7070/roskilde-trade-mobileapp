package dk.rosswap.mobile.feature.auth.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.core.data.UserDto as CoreUserDto
import dk.rosswap.mobile.core.mappers.UserMapper as CoreUserMapper
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import dk.rosswap.mobile.feature.auth.domain.GoogleSignInUseCase
import dk.rosswap.mobile.feature.auth.domain.SignUpUseCase
import dk.rosswap.mobile.feature.account.domain.AccountMapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val signUpUseCase: SignUpUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase
) : AuthRepository {

    companion object {
        private const val TAG = "FirebaseAuthRepo"
        private const val USERS_COLLECTION = "users"
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val userCred = auth.signInWithEmailAndPassword(email, password).await()
            userCred.user ?: return Result.failure(IllegalStateException("No user returned"))
            Log.d(TAG, "Login successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signUp(
        email: String,
        password: String,
        name: String,
        hasConsent: Boolean
    ): Result<Unit> {
        // delegate to use-case
        return signUpUseCase(email, password, name, hasConsent)
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        // delegate to use-case
        return googleSignInUseCase(idToken)
    }

    override suspend fun enrichUserWithFirestoreData(baseUser: User): User {
        return try {
            // get user data from Firestore
            val snap = firestore.collection(USERS_COLLECTION)
                .document(baseUser.uid)
                .get()
                .await()

            if (snap.exists()) {
                // firestore data exist, map to domain model
                val data = snap.data ?: emptyMap<String, Any?>()
                val coreDto = CoreUserDto(
                    uid = (data["uid"] as? String) ?: snap.id,
                    name = data["name"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    photoURL = data["photoURL"] as? String ?: "",
                    createdAt = data["createdAt"] as? com.google.firebase.Timestamp,
                    gdprConsent = data["gdprConsent"] as? Boolean ?: false,
                    consentedAt = data["consentedAt"] as? com.google.firebase.Timestamp,
                    likedItemIds = (data["likedItemIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    dislikedItemIds = (data["dislikedItemIds"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    emailVerified = data["emailVerified"] as? Boolean ?: false,
                    isAnonymous = data["isAnonymous"] as? Boolean ?: false
                )
                return CoreUserMapper.fromDto(coreDto)
            } else {
                baseUser
            }
        } catch (error: Exception) {
            Log.w(TAG, "Failed to fetch user data from Firestore: ${error.message}")
            baseUser
        }
    }

    override fun authUserFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = callbackFlow {
        // listen to auth state changes
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)

        trySend(auth.currentUser)

        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    override fun currentFirebaseUser(): com.google.firebase.auth.FirebaseUser? = auth.currentUser

    override suspend fun signOut(): Result<Unit> {
        return try {
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-out failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(request: UserProfileChangeRequest): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            user.updateProfile(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Update profile failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteCurrentUser(): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            user.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Delete user failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}

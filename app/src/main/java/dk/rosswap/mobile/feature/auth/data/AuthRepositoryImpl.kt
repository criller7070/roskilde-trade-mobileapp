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
import dk.rosswap.mobile.feature.auth.domain.LoginUseCase
import dk.rosswap.mobile.feature.auth.domain.SignOutUseCase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Repo, simple as. This feature uses User data model and Dto in /core/models instead
// of having a domain model and DTO, since other features (e.g. account) uses it

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val signUpUseCase: SignUpUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase,
    private val loginUseCase: LoginUseCase,
    private val signOutUseCase: SignOutUseCase
) : AuthRepository {

    companion object {
        private const val TAG = "FirebaseAuthRepo"
        private const val USERS_COLLECTION = "users"
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return loginUseCase(email, password)
    }

    override suspend fun signUp(
        email: String,
        password: String,
        name: String,
        hasConsent: Boolean
    ): Result<Unit> {
        return signUpUseCase(name, email, password, hasConsent)
    }

    override suspend fun signInWithGoogle(idToken: String, hasConsent: Boolean): Result<Unit> {
        return googleSignInUseCase(idToken, hasConsent)
    }

    // you can debate whether the following two functions should be its own use case. What the
    // responsibilities are for repositories varies a lot between teams; we value simplicity and
    // elegance, with minimal wrapper files.

    // The rules of thumb are that 1: if it's a use case likely used by a user, then it should have
    // its own use case. 2: If it's behind-the-scenes logic, it should be in a repository. 3: If
    // Its simple CRUD, then it should also be in a repository to not clog up the domain directory.
    // That said giving them its own use case is entirely valid if the function is huge.
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
        return signOutUseCase()
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

package dk.rosswap.mobile.feature.auth.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.core.data.UserDto as CoreUserDto
import dk.rosswap.mobile.core.mappers.UserMapper as CoreUserMapper
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import dk.rosswap.mobile.feature.account.domain.AccountMapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
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
        return try {
            // 1. Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("No user returned from sign-up"))

            // 2. Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // 3. Create domain user and write using AccountMapper.toMap
            val domainUser = User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                photoURL = "",
                createdAt = com.google.firebase.Timestamp.now(),
                gdprConsent = hasConsent,
                consentedAt = if (hasConsent) com.google.firebase.Timestamp.now() else null,
                likedItemIds = emptyList(),
                dislikedItemIds = emptyList(),
                emailVerified = firebaseUser.isEmailVerified,
                isAnonymous = false
            )

            val userData = AccountMapper.toMap(domainUser)

            firestore.collection("users").document(firebaseUser.uid).set(userData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-up failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            // just run the standard google auth methods
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()

            Log.d(TAG, "Google sign-in successful")

            // now upload metadata
            val firebaseUser = authResult.user ?: throw Exception("No user returned from sign-in")
            val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false

            if (isNewUser) {
                try {
                    createGoogleUserDoc(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email, firebaseUser.photoUrl?.toString(), hasConsent = false)
                } catch (e: Exception) {
                    Log.w(TAG, "Google sign-in succeeded but creating user doc failed: ${e.message}", e)
                }
            } else { // user already exists, update
                try {
                    updateExistingUserIfNeeded(firebaseUser.uid)
                } catch (e: Exception) {
                    Log.w(TAG, "Google sign-in succeeded but updating existing user failed: ${e.message}", e)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed: ${e.message}", e)
            Result.failure(e)
        }
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

    private suspend fun createGoogleUserDoc(
        uid: String,
        name: String?,
        email: String?,
        photoURL: String?,
        hasConsent: Boolean
    ) {
        try {
            val domainUser = User(
                uid = uid,
                name = (name ?: ""),
                email = (email ?: ""),
                photoURL = (photoURL ?: ""),
                createdAt = com.google.firebase.Timestamp.now(),
                gdprConsent = hasConsent,
                consentedAt = if (hasConsent) com.google.firebase.Timestamp.now() else null,
                likedItemIds = emptyList(),
                dislikedItemIds = emptyList(),
                emailVerified = false,
                isAnonymous = false
            )

            val userData = AccountMapper.toMap(domainUser)

            firestore.collection("users").document(uid).set(userData).await()
            Log.d(TAG, "Google user document created")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Google user document: ${e.message}", e)
            throw e
        }
    }

    private suspend fun updateExistingUserIfNeeded(uid: String) {
        try {
            val userRef = firestore.collection("users").document(uid)
            val userSnap = userRef.get().await()

            if (!userSnap.exists()) {
                // No Firestore doc - create one
                createGoogleUserDoc(uid, null, null, null, hasConsent = true)
            } else {
                val userData = userSnap.data
                if (userData != null && userData["gdprConsent"] == null) {
                    // Missing GDPR consent - add it (assume they consented previously)
                    val updateData = mapOf(
                        "gdprConsent" to true,
                        "consentedAt" to Date()
                    )
                    userRef.set(updateData, SetOptions.merge()).await()
                    Log.d(TAG, "Updated existing user with GDPR consent")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating existing user: ${e.message}", e)
        }
    }
}

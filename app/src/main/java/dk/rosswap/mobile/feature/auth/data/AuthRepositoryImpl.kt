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
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
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
            // Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("No user returned from sign-up"))

            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Create user document in Firestore with GDPR consent
            val userData = hashMapOf(
                "uid" to firebaseUser.uid,
                "name" to name,
                "email" to email,
                "photoURL" to "",
                "createdAt" to Date(),
                "gdprConsent" to hasConsent,
                "consentedAt" to if (hasConsent) Date() else null,
                "likedItemIds" to emptyList<String>(),
                "dislikedItemIds" to emptyList<String>()
            )

            firestore.collection("users").document(firebaseUser.uid).set(userData).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-up failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()

            Log.d(TAG, "Google sign-in successful")

            val firebaseUser = authResult.user ?: throw Exception("No user returned from sign-in")
            val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false

            if (isNewUser) {
                createGoogleUserDoc(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email, firebaseUser.photoUrl?.toString(), hasConsent = false)
            } else {
                updateExistingUserIfNeeded(firebaseUser.uid)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun enrichUserWithFirestoreData(baseUser: User): User {
        return try {
            /* ---------- pull extra data from Firestore ---------- */
            val snap = firestore.collection(USERS_COLLECTION)
                .document(baseUser.uid)
                .get()
                .await()

            if (snap.exists()) {
                /* Convert Firestore document to core DTO and then to domain User */
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
            // Handle Firestore errors gracefully to prevent crashes
            Log.w(TAG, "Failed to fetch user data from Firestore: ${error.message}")
            // Continue with base user data even if Firestore call fails
            baseUser
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
            val userData = hashMapOf(
                "uid" to uid,
                "name" to (name ?: ""),
                "email" to (email ?: ""),
                "photoURL" to (photoURL ?: ""),
                "createdAt" to Date(),
                "gdprConsent" to hasConsent,
                "consentedAt" to if (hasConsent) Date() else null,
                "likedItemIds" to emptyList<String>(),
                "dislikedItemIds" to emptyList<String>()
            )

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

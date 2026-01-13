// kotlin
package dk.rosswap.mobile.feature.auth.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.core.data.UserDto as CoreUserDto
import dk.rosswap.mobile.core.mappers.UserMapper as CoreUserMapper
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthRepository @Inject constructor(
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
            val user = userCred.user ?: return Result.failure(IllegalStateException("No user returned"))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
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
}

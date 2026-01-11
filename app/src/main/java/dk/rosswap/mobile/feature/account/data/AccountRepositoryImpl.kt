package dk.rosswap.mobile.feature.account.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.account.domain.AccountRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AccountRepository {

    companion object {
        private const val TAG = "FirebaseAccountRepo"
    }

    override suspend fun createAccount(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Result<Unit> {
        try {
            val userCred = auth.createUserWithEmailAndPassword(email, password).await()
            val user = userCred.user ?: throw IllegalStateException("No user returned")

            try {
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profile).await()
                user.sendEmailVerification().await()

                val userDoc = mapOf(
                    "uid" to user.uid,
                    "name" to name,
                    "email" to email.lowercase(),
                    "createdAt" to Timestamp.now(),
                    "consentedAt" to Timestamp.now(),
                    "gdprConsent" to true,
                    "emailVerified" to user.isEmailVerified
                )

                firestore.collection("users").document(user.uid).set(userDoc).await()
                return Result.success(Unit)
            } catch (e: Exception) {
                try {
                    user.delete().await()
                } catch (deleteException: Exception) {
                    Log.e(TAG, "Failed to delete user during cleanup", deleteException)
                }
                return Result.failure(e)
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun addLikedItem(itemId: String): Result<Unit> {
        val currentUser = auth.currentUser ?: return Result.failure(IllegalStateException("No user logged in"))
        return try {
            firestore.collection("users").document(currentUser.uid)
                .update("likedItemIds", FieldValue.arrayUnion(itemId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add liked item", e)
            Result.failure(e)
        }
    }
}

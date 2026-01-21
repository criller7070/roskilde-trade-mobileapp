package dk.rosswap.mobile.feature.auth.domain

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.account.domain.AccountMapper
import dk.rosswap.mobile.core.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "SignUpUseCase"
        private const val USERS_COLLECTION = "users"
    }

    suspend operator fun invoke(
        email: String,
        password: String,
        name: String,
        hasConsent: Boolean
    ): Result<Unit> {
        return try {
            // Create user in Firebase Auth
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("No user returned from sign-up"))

            // Update display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Create domain User and write to Firestore using AccountMapper.toMap
            val domainUser = User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                photoURL = "",
                createdAt = Timestamp.now(),
                gdprConsent = hasConsent,
                consentedAt = if (hasConsent) Timestamp.now() else null,
                likedItemIds = emptyList(),
                dislikedItemIds = emptyList(),
                emailVerified = firebaseUser.isEmailVerified,
                isAnonymous = false
            )

            val userData = AccountMapper.toMap(domainUser)

            firestore.collection(USERS_COLLECTION).document(firebaseUser.uid).set(userData).await()

            Log.d(TAG, "Sign-up user document created")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-up failed: ${e.message}", e)
            Result.failure(e)
        }
    }
}

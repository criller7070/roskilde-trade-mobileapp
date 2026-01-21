package dk.rosswap.mobile.feature.account.domain

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.utils.EmailValidatorUtil
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.model.User
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Use case both invoking and housing the logic for createAccount function in repo.
// Many apps have the logic directly in the repo, but we avoid that here for clarity.

class CreateAccountUseCase @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) {

    companion object {
        private const val TAG = "CreateAccountUseCase"
    }

    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Result<Unit> {

        // Check that they accepted terms!
        if (!acceptedTerms) return Result.failure(IllegalStateException("Terms not accepted"))
        val errors = mutableListOf<String>()
        if (name.isBlank()) errors.add("Name must not be blank")

        // Check valid email
        val emailValidation = EmailValidatorUtil.validate(email)
        if (!emailValidation.isValid) {
            return Result.failure(IllegalArgumentException(emailValidation.message))
        }

        // Check password
        if (password.length < 6) errors.add("Password must be at least 6 characters long")
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException(errors.joinToString("; ")))

        // If passed checks: Create user in Firebase Auth
        try {
            val userCred = auth.createUserWithEmailAndPassword(email, password).await()
            val user = userCred.user ?: throw IllegalStateException("No user returned")

            try {
                // 1. Update display name
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                // 2. Update profile
                val profileResult = sessionManager.updateProfile(profile)
                if (profileResult.isFailure) {
                    throw profileResult.exceptionOrNull() ?: IllegalStateException("Failed to update profile")
                }

                // 3. Send verification email, wait for result...
                user.sendEmailVerification().await()

                // 4. write to Firestore
                val domainUser = User(
                    uid = user.uid,
                    name = name,
                    email = email,
                    photoURL = "",
                    createdAt = Timestamp.now(),
                    gdprConsent = true,
                    consentedAt = Timestamp.now(),
                    likedItemIds = emptyList(),
                    dislikedItemIds = emptyList(),
                    emailVerified = user.isEmailVerified, // result of email
                    isAnonymous = false
                )

                // 5. Convert User to Map
                val userDoc = AccountMapper.toMap(domainUser)

                // 6. Finally, write to Firestore
                firestore.collection("users").document(user.uid).set(userDoc).await()
                return Result.success(Unit)
            } catch (e: Exception) {
                try {
                    val delRes = sessionManager.deleteAccount()
                    if (delRes.isFailure) {
                        Log.e(TAG, "Failed to delete user during cleanup", delRes.exceptionOrNull())
                    }
                } catch (deleteException: Exception) {
                    Log.e(TAG, "Failed to delete user during cleanup", deleteException)
                }
                return Result.failure(e)
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}

package dk.rosswap.mobile.feature.auth.domain

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.utils.EmailValidatorUtil
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.feature.account.domain.AccountMapper
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    companion object {
        private const val TAG = "SignUpUseCase"
    }

    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Result<Unit> {
        // check that they accepted terms!
        if (!acceptedTerms) return Result.failure(IllegalStateException("Terms not accepted"))
        val errors = mutableListOf<String>()
        if (name.isBlank()) errors.add("Name must not be blank")

        // check valid email w our util
        val emailValidation = EmailValidatorUtil.validate(email)
        if (!emailValidation.isValid) {
            return Result.failure(IllegalArgumentException(emailValidation.message))
        }

        // check password
        if (password.length < 6) errors.add("Password must be at least 6 characters long")
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException(errors.joinToString("; ")))

        // If passed checks: Create user in Firebase Auth
        try {
            val userCred = auth.createUserWithEmailAndPassword(email, password).await()
            val user = userCred.user ?: throw IllegalStateException("No user returned")

            try {
                // 1. Update display name directly on the Firebase user
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()

                user.updateProfile(profile).await()

                // 2. Send verification email
                user.sendEmailVerification().await()

                // 3. write to Firestore. Again, there has to be a better way to map this
                val domainUser = User(
                    uid = user.uid,
                    name = name,
                    email = email,
                    photoURL = "",
                    createdAt = Timestamp.now(), // TODO: Standardize timestamp
                    gdprConsent = true,
                    consentedAt = Timestamp.now(),
                    likedItemIds = emptyList(),
                    dislikedItemIds = emptyList(),
                    emailVerified = user.isEmailVerified, // result of email
                    isAnonymous = false
                )

                // 4. Convert User to Map
                val userDoc = AccountMapper.toMap(domainUser)

                // 5. Finally, write to Firestore
                firestore.collection("users").document(user.uid).set(userDoc).await()
                return Result.success(Unit)
            } catch (e: Exception) {
                // Cleanup: try to delete the partially created Firebase user
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
}

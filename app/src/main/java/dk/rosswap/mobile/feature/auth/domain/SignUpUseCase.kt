package dk.rosswap.mobile.feature.auth.domain

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        name: String,
        hasConsent: Boolean
    ): Result<Unit> {
        return authRepository.signUp(email, password, name, hasConsent)
    }
}

/**
 * Internal implementation for signing up in Firebase
 * Used by AuthRepository
 */
internal suspend fun firebaseSignUp(
    email: String,
    password: String,
    name: String,
    hasConsent: Boolean,
    firebaseAuth: FirebaseAuth,
    firestore: FirebaseFirestore
): Result<Unit> {
    return try {
        // Create user in Firebase Auth
        val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = authResult.user ?: throw Exception("No user returned from sign-up")

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
        Result.failure(e)
    }
}

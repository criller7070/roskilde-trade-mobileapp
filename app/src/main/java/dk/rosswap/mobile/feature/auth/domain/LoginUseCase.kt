package dk.rosswap.mobile.feature.auth.domain

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        return try {
            val userCred = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            userCred.user ?: return Result.failure(IllegalStateException("No user returned"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

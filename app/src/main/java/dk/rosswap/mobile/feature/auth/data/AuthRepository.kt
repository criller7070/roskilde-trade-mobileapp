// kotlin
package dk.rosswap.mobile.feature.auth.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRepository {

    companion object {
        private const val TAG = "FirebaseAuthRepo"
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
}

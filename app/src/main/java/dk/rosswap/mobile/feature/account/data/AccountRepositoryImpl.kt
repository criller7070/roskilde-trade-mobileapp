package dk.rosswap.mobile.feature.account.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.account.domain.AccountMapper
import dk.rosswap.mobile.feature.account.domain.AccountRepository
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
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

                val userDoc = AccountMapper.createUserDocMap(
                    uid = user.uid,
                    name = name,
                    email = email,
                    emailVerified = user.isEmailVerified
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
}

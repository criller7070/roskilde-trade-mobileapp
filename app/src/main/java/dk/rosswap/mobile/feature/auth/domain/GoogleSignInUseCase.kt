package dk.rosswap.mobile.feature.auth.domain

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.rosswap.mobile.R
import dk.rosswap.mobile.feature.account.domain.AccountMapper
import dk.rosswap.mobile.core.model.User
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class GoogleSignInUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "GoogleSignInUseCase"
        private const val USERS_COLLECTION = "users"
    }

    suspend operator fun invoke(idToken: String): Result<Unit> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()

            Log.d(TAG, "Google sign-in successful")

            val firebaseUser = authResult.user ?: throw Exception("No user returned from sign-in")
            val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false

            if (isNewUser) {
                try {
                    createGoogleUserDoc(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email, firebaseUser.photoUrl?.toString(), hasConsent = false)
                } catch (e: Exception) {
                    Log.w(TAG, "Google sign-in succeeded but creating user doc failed: ${e.message}", e)
                }
            } else {
                try {
                    updateExistingUserIfNeeded(firebaseUser.uid)
                } catch (e: Exception) {
                    Log.w(TAG, "Google sign-in succeeded but updating existing user failed: ${e.message}", e)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }

    private suspend fun createGoogleUserDoc(
        uid: String,
        name: String?,
        email: String?,
        photoURL: String?,
        hasConsent: Boolean
    ) {
        try {
            val domainUser = User(
                uid = uid,
                name = (name ?: ""),
                email = (email ?: ""),
                photoURL = (photoURL ?: ""),
                createdAt = com.google.firebase.Timestamp.now(),
                gdprConsent = hasConsent,
                consentedAt = if (hasConsent) com.google.firebase.Timestamp.now() else null,
                likedItemIds = emptyList(),
                dislikedItemIds = emptyList(),
                emailVerified = false,
                isAnonymous = false
            )

            val userData = AccountMapper.toMap(domainUser)

            firestore.collection(USERS_COLLECTION).document(uid).set(userData).await()
            Log.d(TAG, "Google user document created")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Google user document: ${e.message}", e)
            throw e
        }
    }

    private suspend fun updateExistingUserIfNeeded(uid: String) {
        try {
            val userRef = firestore.collection(USERS_COLLECTION).document(uid)
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
                    userRef.set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
                    Log.d(TAG, "Updated existing user with GDPR consent")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating existing user: ${e.message}", e)
        }
    }
}

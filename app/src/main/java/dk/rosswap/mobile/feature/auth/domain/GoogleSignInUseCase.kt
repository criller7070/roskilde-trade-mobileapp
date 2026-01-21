package dk.rosswap.mobile.feature.auth.domain

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.rosswap.mobile.core.mappers.UserMapper as CoreUserMapper
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

    // generally, signing in with google can either be magically easy or abysmally faulty,
    // so there's quite a few more logs in this method for debugging. Its worth keeping also
    suspend operator fun invoke(idToken: String): Result<Unit> {
        return try {
            // 1. get info from Google
            Log.d(TAG, "Attempting Google sign-in")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()

            Log.d(TAG, "Google sign-in successful")

            // 2. check if user is new
            val firebaseUser = authResult.user ?: throw Exception("No user returned from sign-in")
            val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false

            if (isNewUser) { // 3A:  if new...
                try {
                    createGoogleUserDoc(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email, firebaseUser.photoUrl?.toString(), hasConsent = false)
                } catch (e: Exception) {
                    Log.w(TAG, "Google sign-in succeeded but creating user doc failed: ${e.message}", e)
                }
            } else { // 3B: if not...
                try {
                    updateExistingUser(firebaseUser.uid)
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

    private suspend fun createGoogleUserDoc(
        uid: String,
        name: String?,
        email: String?,
        photoURL: String?,
        hasConsent: Boolean
    ) {
        try {
            // there's probably a more intelligent way to map this, but even with the mappers
            // in core I can't quite seem to find a good solution. It's fine for now.
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

            val userDto = CoreUserMapper.toDto(domainUser)
            val userData = userDto.copy(email = userDto.email.lowercase())

            firestore.collection(USERS_COLLECTION).document(uid).set(userData).await()
            Log.d(TAG, "Google user document created")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Google user document: ${e.message}", e)
            throw e
        }
    }

    private suspend fun updateExistingUser(uid: String) {
        try {
            // 1. get info
            val userRef = firestore.collection(USERS_COLLECTION).document(uid)
            val userSnap = userRef.get().await() // i.e. snapshot

            if (!userSnap.exists()) { // 2A. if not exists...
                createGoogleUserDoc(uid, null, null, null, hasConsent = true)
            } else { // 2B. if exists...
                val userData = userSnap.data
                if (userData != null && userData["gdprConsent"] == null) {
                    // 3. Missing GDPR consent
                    val updateData = mapOf(
                        "gdprConsent" to true, // we assume they've consented already
                        "consentedAt" to Date()
                    )
                    // 4. Update in Firestore
                    userRef.set(updateData, com.google.firebase.firestore.SetOptions.merge()).await()
                    Log.d(TAG, "Updated existing user with GDPR consent")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating existing user: ${e.message}", e)
        }
    }
}

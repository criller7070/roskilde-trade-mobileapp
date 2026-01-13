package dk.rosswap.mobile.feature.auth.data

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import dk.rosswap.mobile.core.common.User
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Helper for Google Sign-In flow.
 * Mirrors the web app's signInWithGoogle() and createGoogleUser() functions.
 */
class GoogleSignInHelper(private val context: Context) {
    private const val TAG = "GoogleSignInHelper"

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("") // Add your web client ID from google-services.json
            .requestEmail()
            .requestProfile()
            .build()
        
        GoogleSignIn.getClient(context, gso)
    }

    /**
     * Signs in with Google and creates/updates user document in Firestore.
     * Mirrors the web app's signInWithGoogle() function.
     *
     * @param idToken The ID token from Google Sign-In
     * @return Result containing the auth result and whether user is new
     */
    suspend fun signInWithGoogle(idToken: String): Result<GoogleSignInResult> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = FirebaseInitializer.auth.signInWithCredential(credential).await()
            
            Log.d(TAG, "Google sign-in successful")
            
            val firebaseUser = authResult.user ?: throw Exception("No user returned from sign-in")
            val isNewUser = authResult.additionalUserInfo?.isNewUser ?: false
            
            // Create or update user document in Firestore
            if (isNewUser) {
                createGoogleUserDoc(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email, firebaseUser.photoUrl?.toString(), hasConsent = false)
            } else {
                // For existing users, ensure GDPR fields are present
                updateExistingUserIfNeeded(firebaseUser.uid, firebaseUser.displayName, firebaseUser.email, firebaseUser.photoUrl?.toString())
            }
            
            Result.success(
                GoogleSignInResult(
                    user = firebaseUser,
                    isNewUser = isNewUser
                )
            )
        } catch (e: ApiException) {
            Log.e(TAG, "Google sign-in failed with code: ${e.statusCode}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a new Google user document in Firestore with GDPR consent field.
     * Mirrors the web app's createGoogleUser() function.
     */
    private suspend fun createGoogleUserDoc(
        uid: String,
        name: String?,
        email: String?,
        photoURL: String?,
        hasConsent: Boolean
    ) {
        try {
            val userData = hashMapOf(
                "uid" to uid,
                "name" to (name ?: ""),
                "email" to (email ?: ""),
                "photoURL" to (photoURL ?: ""),
                "createdAt" to Date(),
                "gdprConsent" to hasConsent,
                "consentedAt" to if (hasConsent) Date() else null,
                "likedItemIds" to emptyList<String>(),
                "dislikedItemIds" to emptyList<String>()
            )
            
            FirebaseInitializer.firestore.collection("users").document(uid).set(userData).await()
            Log.d(TAG, "Google user document created")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Google user document: ${e.message}", e)
            throw e
        }
    }

    /**
     * Updates an existing user document with GDPR consent fields if missing.
     * Handles migration of existing users.
     */
    private suspend fun updateExistingUserIfNeeded(
        uid: String,
        name: String?,
        email: String?,
        photoURL: String?
    ) {
        try {
            val userRef = FirebaseInitializer.firestore.collection("users").document(uid)
            val userSnap = userRef.get().await()
            
            if (!userSnap.exists()) {
                // No Firestore doc - create one
                createGoogleUserDoc(uid, name, email, photoURL, hasConsent = true)
            } else {
                val userData = userSnap.data
                if (userData != null && userData["gdprConsent"] == null) {
                    // Missing GDPR consent - add it (assume they consented previously)
                    val updateData = mapOf(
                        "gdprConsent" to true,
                        "consentedAt" to Date()
                    )
                    userRef.set(updateData, SetOptions.merge()).await()
                    Log.d(TAG, "Updated existing user with GDPR consent")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating existing user: ${e.message}", e)
            // Don't throw - allow user to continue even if Firestore update fails
        }
    }

    /**
     * Result data class for Google Sign-In
     */
    data class GoogleSignInResult(
        val user: com.google.firebase.auth.FirebaseUser,
        val isNewUser: Boolean
    )
}

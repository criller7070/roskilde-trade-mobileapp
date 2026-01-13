package dk.rosswap.mobile.feature.auth.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.common.User
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import dk.rosswap.mobile.feature.auth.domain.firebaseSignUp
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Implementation of AuthRepository using Firebase Auth and Firestore.
 * Mirrors the web app's authentication patterns using Kotlin coroutines.
 */
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val googleSignInHelper: GoogleSignInHelper
) : AuthRepository {

    companion object {
        private const val TAG = "FirebaseAuthRepo"
        private const val USERS_COLLECTION = "users"
    }

    /**
     * Logs in user with email and password.
     */
    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val userCred = auth.signInWithEmailAndPassword(email, password).await()
            val user = userCred.user ?: return Result.failure(IllegalStateException("No user returned"))
            Log.d(TAG, "Login successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Login failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs up a new user with email and password.
     * Mirrors the web app's handleRegister() function.
     */
    override suspend fun signUp(
        email: String,
        password: String,
        name: String,
        hasConsent: Boolean
    ): Result<Unit> {
        return try {
            firebaseSignUp(email, password, name, hasConsent)
        } catch (e: Exception) {
            Log.e(TAG, "Sign-up failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Signs in with Google and creates/updates user document in Firestore.
     * Mirrors the web app's handleGoogleSignIn() function.
     */
    override suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        return try {
            googleSignInHelper.signInWithGoogle(idToken).getOrThrow()
            Log.d(TAG, "Google sign-in successful")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Google sign-in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches additional user data from Firestore and enriches the User object.
     * Mirrors the React pattern: listen → fetch → enrich → emit state
     */
    override suspend fun enrichUserWithFirestoreData(baseUser: User): User {
        return try {
            /* ---------- pull extra data from Firestore ---------- */
            val snap = firestore.collection(USERS_COLLECTION)
                .document(baseUser.uid)
                .get()
                .await()

            if (snap.exists()) {
                /* Convert Firestore document to User object with all fields */
                val enrichedUser = snap.toObject(User::class.java)
                if (enrichedUser == null) {
                    Log.w(
                        TAG,
                        "Failed to deserialize Firestore user document for uid=${baseUser.uid}; falling back to baseUser"
                    )
                    baseUser
                } else {
                    enrichedUser
                }
            } else {
                baseUser
            }
        } catch (error: Exception) {
            // Handle Firestore errors gracefully to prevent crashes
            Log.w(TAG, "Failed to fetch user data from Firestore: ${error.message}")
            // Continue with base user data even if Firestore call fails
            baseUser
        }
    }
}

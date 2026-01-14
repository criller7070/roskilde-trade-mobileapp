package dk.rosswap.mobile.feature.auth.domain

import dk.rosswap.mobile.core.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>

    /**
     * Signs up a new user with email and password.
     * Mirrors the web app's handleRegister() function.
     */
    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        hasConsent: Boolean
    ): Result<Unit>

    /**
     * Signs in with Google and creates/updates user document in Firestore.
     * Mirrors the web app's handleGoogleSignIn() function.
     */
    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    /**
     * Fetches additional user data from Firestore and enriches the User object.
     * Mirrors the React pattern: listen → fetch → enrich → emit state
     *
     * @param baseUser The base user object (from Firebase Auth)
     * @return Enriched User object with Firestore data
     */
    suspend fun enrichUserWithFirestoreData(baseUser: User): User
}
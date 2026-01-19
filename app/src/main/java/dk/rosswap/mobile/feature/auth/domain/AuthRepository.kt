package dk.rosswap.mobile.feature.auth.domain

import dk.rosswap.mobile.core.model.User

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        hasConsent: Boolean
    ): Result<Unit>

    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    suspend fun enrichUserWithFirestoreData(baseUser: User): User
}
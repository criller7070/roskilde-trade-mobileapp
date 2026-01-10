package dk.rosswap.mobile.feature.auth.domain

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<Unit>
}
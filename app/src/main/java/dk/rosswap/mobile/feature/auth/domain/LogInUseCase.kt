package dk.rosswap.mobile.feature.auth.domain

import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        val errors = mutableListOf<String>()
        if (email.isBlank()) errors.add("Email must not be blank")
        if (password.isBlank()) errors.add("Password must not be blank")
        if (password.length < 6) errors.add("Password must be at least 6 characters long")
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException(errors.joinToString("; ")))

        return repository.login(email.trim(), password)
    }
}
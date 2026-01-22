package dk.rosswap.mobile.feature.auth.domain

import javax.inject.Inject

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        name: String,
        hasConsent: Boolean
    ): Result<Unit> {
        return authRepository.signUp(email, password, name, hasConsent)
    }
}

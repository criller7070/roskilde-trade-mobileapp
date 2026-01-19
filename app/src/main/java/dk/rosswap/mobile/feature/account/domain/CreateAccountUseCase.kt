package dk.rosswap.mobile.feature.account.domain

import dk.rosswap.mobile.core.utils.EmailValidator
import javax.inject.Inject

class CreateAccountUseCase @Inject constructor(
    private val repository: AccountRepository
) {
    suspend operator fun invoke(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Result<Unit> {
        if (!acceptedTerms) return Result.failure(IllegalStateException("Terms not accepted"))
        val errors = mutableListOf<String>()
        if (name.isBlank()) errors.add("Name must not be blank")

        val emailValidation = EmailValidator.validate(email)
        if (!emailValidation.isValid) {
            return Result.failure(IllegalArgumentException(emailValidation.message))
        }

        if (password.length < 6) errors.add("Password must be at least 6 characters long")
        if (errors.isNotEmpty()) return Result.failure(IllegalArgumentException(errors.joinToString("; ")))

        return repository.createAccount(name, email, password, acceptedTerms)
    }
}

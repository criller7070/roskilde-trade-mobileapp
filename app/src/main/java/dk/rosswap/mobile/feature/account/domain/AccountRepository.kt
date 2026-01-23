package dk.rosswap.mobile.feature.account.domain

// AccountRepository interface. Right now it just CreateAccount use case, but should
// it have more, like UpdateAccountUseCase, it can be added to the interface

interface AccountRepository {
    suspend fun createAccount(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Result<Unit>
}

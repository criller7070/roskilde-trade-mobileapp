package dk.rosswap.mobile.feature.account.domain

interface AccountRepository {
    suspend fun createAccount(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Result<Unit>
}

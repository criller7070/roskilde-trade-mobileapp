package dk.rosswap.mobile.feature.account.data

import dk.rosswap.mobile.feature.account.domain.AccountRepository
import javax.inject.Inject
import dk.rosswap.mobile.feature.account.domain.CreateAccountUseCase

// Repository, i.e. the class responsible for communicating directly with firebase.

class AccountRepositoryImpl @Inject constructor(
    private val createAccountUseCase: CreateAccountUseCase
) : AccountRepository {

    override suspend fun createAccount(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ): Result<Unit> {
        return createAccountUseCase(name, email, password, acceptedTerms)
    }
}

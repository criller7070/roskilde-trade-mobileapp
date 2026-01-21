package dk.rosswap.mobile.feature.account.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.feature.account.data.AccountRepositoryImpl
import dk.rosswap.mobile.feature.account.domain.AccountRepository
import javax.inject.Singleton

@Suppress("unused") // intentional, used by Hilt
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository
}

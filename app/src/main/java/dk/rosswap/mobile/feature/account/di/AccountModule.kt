package dk.rosswap.mobile.feature.account.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

import dk.rosswap.mobile.feature.account.data.AccountRepositoryImpl
import dk.rosswap.mobile.feature.account.domain.AccountRepository

@Suppress("unused") // used by Hilt
@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: AccountRepositoryImpl
    ): AccountRepository
}

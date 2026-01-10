package dk.rosswap.mobile.feature.account.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(
        impl: dk.rosswap.mobile.feature.account.data.AccountRepository // This should be changed ong
    ): dk.rosswap.mobile.feature.account.domain.AccountRepository // weird import type shit problem
    // TODO fix shit fucking import naming problem
}

package dk.rosswap.mobile.feature.account.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.feature.account.data.FirebaseAccountRepository
import dk.rosswap.mobile.feature.account.domain.AccountRepository
import dk.rosswap.mobile.feature.account.domain.CreateAccountUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountModule {

    @Provides
    @Singleton
    fun provideAccountRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): AccountRepository = FirebaseAccountRepository(auth, firestore)

    @Provides
    fun provideCreateAccountUseCase(repository: AccountRepository): CreateAccountUseCase =
        CreateAccountUseCase(repository)
}


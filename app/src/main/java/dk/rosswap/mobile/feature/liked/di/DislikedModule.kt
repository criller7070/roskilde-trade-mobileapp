package dk.rosswap.mobile.feature.liked.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.feature.liked.data.DislikedRepositoryImpl
import dk.rosswap.mobile.feature.liked.domain.DislikedRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DislikedModule {

    @Binds
    @Singleton
    abstract fun bindDislikedRepository(
        impl: DislikedRepositoryImpl
    ): DislikedRepository
}

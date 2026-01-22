package dk.rosswap.mobile.feature.liked.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.feature.liked.data.LikedRepositoryImpl
import dk.rosswap.mobile.feature.liked.domain.LikedRepository
import javax.inject.Singleton

@Suppress("unused") // intentional
@Module
@InstallIn(SingletonComponent::class)
abstract class LikedModule {

    @Binds
    @Singleton
    abstract fun bindLikedRepository(
        impl: LikedRepositoryImpl
    ): LikedRepository
}

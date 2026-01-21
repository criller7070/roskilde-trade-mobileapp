package dk.rosswap.mobile.feature.items.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.feature.items.data.ItemsRepositoryImpl
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
abstract class ItemsModule {

    @Binds
    @Singleton
    abstract fun bindItemsRepository(
        impl: ItemsRepositoryImpl
    ): ItemsRepository
}

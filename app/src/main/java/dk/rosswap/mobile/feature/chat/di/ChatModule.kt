package dk.rosswap.mobile.feature.chat.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.feature.chat.data.ChatRepositoryImpl
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import javax.inject.Singleton

// as always we bind repo interface to repo implementation in Hilt

@Suppress("unused") // intentional
@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository
}

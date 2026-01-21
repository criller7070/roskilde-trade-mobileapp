package dk.rosswap.mobile.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.common.SessionManagerImpl
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import javax.inject.Singleton

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideSessionManager(authRepo: AuthRepository): SessionManager {
        return SessionManagerImpl(authRepo)
    }
}

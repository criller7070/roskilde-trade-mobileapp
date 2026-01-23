package dk.rosswap.mobile.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.common.SessionManagerImpl
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext

// these are DI modules for registering objects as singletons and providing them
// for future injection in feature repositories

@Suppress("unused")
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    // in future we can consider giving session manager its own file, SessionModule,
    // but let's just keep it in AuthModule because it's related to authing
    @Provides
    @Singleton
    fun provideSessionManager(authRepo: AuthRepository, @ApplicationContext appContext: Context): SessionManager {
        return SessionManagerImpl(authRepo, appContext)
    }
}

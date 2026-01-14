package dk.rosswap.mobile.feature.auth.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dk.rosswap.mobile.feature.auth.data.GoogleSignInHelper
import dk.rosswap.mobile.feature.auth.data.AuthRepositoryImpl
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import javax.inject.Singleton

/**
 * Hilt dependency injection module for authentication features.
 * Provides Firebase auth services, Google Sign-In helper, and repository implementations.
 * Mirrors the web app's context providers pattern using Kotlin dependency injection.
 */
@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Singleton
    @Provides
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth
    }

    @Singleton
    @Provides
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    @Singleton
    @Provides
    fun provideGoogleSignInHelper(
        @ApplicationContext context: Context,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): GoogleSignInHelper {
        return GoogleSignInHelper(context, firebaseAuth, firestore)
    }

    @Singleton
    @Provides
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        googleSignInHelper: GoogleSignInHelper
    ): AuthRepository {
        return AuthRepositoryImpl(firebaseAuth, firestore, googleSignInHelper)
    }
}

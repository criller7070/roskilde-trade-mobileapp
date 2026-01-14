package dk.rosswap.mobile.feature.auth.domain

import android.content.Context
import dk.rosswap.mobile.feature.auth.data.GoogleSignInHelper
import javax.inject.Inject

/**
 * Use case for Google Sign-In.
 * Mirrors the web app's handleGoogleSignIn() function.
 */
class GoogleSignInUseCase @Inject constructor(
    private val context: Context,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<Unit> {
        return authRepository.signInWithGoogle(idToken)
    }

    fun getGoogleSignInClient() = GoogleSignInHelper(context).googleSignInClient
}

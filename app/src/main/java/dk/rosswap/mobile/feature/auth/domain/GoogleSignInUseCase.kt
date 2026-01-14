package dk.rosswap.mobile.feature.auth.domain

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.rosswap.mobile.R
import javax.inject.Inject

class GoogleSignInUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): Result<Unit> {
        return authRepository.signInWithGoogle(idToken)
    }

    fun getGoogleSignInClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()

        return GoogleSignIn.getClient(context, gso)
    }
}

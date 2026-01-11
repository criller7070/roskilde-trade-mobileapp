package dk.rosswap.mobile.core.common

import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

@ActivityRetainedScoped
class AuthState @Inject constructor(
    private val auth: FirebaseAuth
) {
    val isLoggedInFlow: Flow<Boolean> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser != null)
        }
        auth.addAuthStateListener(listener)
        // emit initial
        trySend(auth.currentUser != null)

        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    fun isLoggedIn(): Boolean = auth.currentUser != null
}
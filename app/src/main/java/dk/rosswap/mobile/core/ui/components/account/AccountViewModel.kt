package dk.rosswap.mobile.core.ui.components.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.MutableLiveData
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    val createResult = MutableLiveData<Result<Unit>>()

    fun createAccount(name: String, email: String, password: String, acceptedTerms: Boolean) {
        if (!acceptedTerms) {
            createResult.postValue(Result.failure(IllegalStateException("Terms not accepted")))
            return
        }
        if (name.isBlank() || email.isBlank() || password.length < 6) {
            createResult.postValue(Result.failure(IllegalArgumentException("Invalid input")))
            return
        }

        viewModelScope.launch {
            try {
                val userCred = auth.createUserWithEmailAndPassword(email, password).await()
                val user = userCred.user ?: throw IllegalStateException("No user returned")

                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profile).await()

                user.sendEmailVerification().await()

                val userDoc = mapOf(
                    "uid" to user.uid,
                    "name" to name,
                    "email" to email.lowercase(),
                    "createdAt" to Timestamp.now(),
                    "consentedAt" to Timestamp.now(),
                    "gdprConsent" to true,
                    "emailVerified" to user.isEmailVerified
                )

                firestore.collection("users").document(user.uid).set(userDoc).await()

                createResult.postValue(Result.success(Unit))
            } catch (e: Exception) {
                createResult.postValue(Result.failure(e))
            }
        }
    }
}

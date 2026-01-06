package dk.rosswap.mobile.core.ui.components.account

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {
    
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _createResult = MutableLiveData<Result<Unit>>()
    val createResult: LiveData<Result<Unit>> = _createResult

    companion object {
        private const val TAG = "AccountViewModel"
    }

    fun createAccount(name: String, email: String, password: String, acceptedTerms: Boolean) {
        // Prevent concurrent account creation attempts
        if (_isLoading.value == true) {
            return
        }

        if (!acceptedTerms) {
            _createResult.postValue(Result.failure(IllegalStateException("Terms not accepted")))
            return
        }
        val errors = mutableListOf<String>()
        if (name.isBlank()) {
            errors.add("Name must not be blank")
        }
        if (email.isBlank()) {
            errors.add("Email must not be blank")
        }
        if (password.length < 6) {
            errors.add("Password must be at least 6 characters long")
        }
        if (errors.isNotEmpty()) {
            _createResult.postValue(
                Result.failure(IllegalArgumentException(errors.joinToString("; ")))
            )
            return
        }

        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val userCred = auth.createUserWithEmailAndPassword(email, password).await()
                val user = userCred.user ?: throw IllegalStateException("No user returned")

                try {
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

                    _createResult.postValue(Result.success(Unit))
                } catch (e: Exception) {
                    // Cleanup: delete the auth user if any subsequent operation fails
                    try {
                        user.delete().await()
                    } catch (deleteException: Exception) {
                        Log.e(TAG, "Failed to delete user during cleanup", deleteException)
                    }
                    throw e
                }
            } catch (e: Exception) {
                _createResult.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}

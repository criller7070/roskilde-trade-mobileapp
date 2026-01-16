package dk.rosswap.mobile.feature.account.presentation

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _photoUrl = MutableLiveData<String?>()
    val photoUrl: LiveData<String?> = _photoUrl

    val user
        get() = (sessionManager.authState.value as? dk.rosswap.mobile.core.common.AuthState.Authenticated)?.user

    init {
        _photoUrl.value = user?.photoURL
    }

    fun uploadProfilePicture(uri: Uri) {
        val currentUser = user ?: return
        val uid = currentUser.uid

        val ref = storage.reference.child("profilePictures/$uid")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    photoUri = downloadUri
                }

                // Use SessionManager helper to update profile instead of calling FirebaseAuth directly
                viewModelScope.launch {
                    val result = sessionManager.updateProfile(profileUpdates)
                    result.fold(
                        onSuccess = { _photoUrl.postValue(downloadUri.toString()) },
                        onFailure = { e -> android.util.Log.e("ProfileViewModel", "Failed to update profile", e) }
                    )
                }
            }
    }

    fun deleteAccount(
        onSuccess: () -> Unit,
        onReauthRequired: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            val result = sessionManager.deleteAccount()
            result.fold(
                onSuccess = { onSuccess() },
                onFailure = { e ->
                    if (e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                        onReauthRequired()
                    } else {
                        onError(e as Exception)
                    }
                }
            )
        }
    }
}

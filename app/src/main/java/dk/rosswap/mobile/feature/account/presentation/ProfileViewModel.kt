package dk.rosswap.mobile.feature.account.presentation

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.storage.FirebaseStorage


class ProfileViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    val user = auth.currentUser

    private val _photoUrl = MutableLiveData<String?>()
    val photoUrl: LiveData<String?> = _photoUrl

    init {
        _photoUrl.value = user?.photoUrl?.toString()
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
                val profileUpdates = userProfileChangeRequest {
                    photoUri = downloadUri
                }

                currentUser.updateProfile(profileUpdates)
                    .addOnSuccessListener {
                        _photoUrl.value = downloadUri.toString()
                    }
            }
    }
    fun deleteAccount(
        onSuccess: () -> Unit,
        onReauthRequired: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser ?: return

        currentUser.delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                // Firebase throws this when re-authentication is required
                if (exception is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                    onReauthRequired()
                } else {
                    onError(exception)
                }
            }
    }

}

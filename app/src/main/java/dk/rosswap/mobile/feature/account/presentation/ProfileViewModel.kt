package dk.rosswap.mobile.feature.account.presentation

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.feature.account.domain.AccountItem
import dk.rosswap.mobile.feature.account.domain.toAccountItem
import dk.rosswap.mobile.feature.items.domain.GetItemsUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val storage: FirebaseStorage,
    private val getItemsUseCase: GetItemsUseCase
) : ViewModel() {

    private val _photoUrl = MutableLiveData<String?>()
    val photoUrl: LiveData<String?> = _photoUrl

    private val _posts = MutableLiveData<List<AccountItem>>(emptyList())
    val posts: LiveData<List<AccountItem>> = _posts

    val user
        get() = (sessionManager.authState.value as? dk.rosswap.mobile.core.common.AuthState.Authenticated)?.user

    init {
        _photoUrl.value = user?.photoURL
        // Load posts on ViewModel init
        loadMyPosts()
    }

    fun loadMyPosts(limit: Long = 50) {
        val uid = sessionManager.currentUserId() ?: run {
            _posts.postValue(emptyList())
            return
        }

        viewModelScope.launch {
            val result = try {
                getItemsUseCase(limit)
            } catch (e: Exception) {
                Result.failure<List<dk.rosswap.mobile.core.model.Item>>(e)
            }

            result.fold(
                onSuccess = { items ->
                    val my = items.filter { it.userId == uid }.map { it.toAccountItem() }
                    _posts.postValue(my)
                },
                onFailure = {
                    _posts.postValue(emptyList())
                }
            )
        }
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

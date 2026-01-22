package dk.rosswap.mobile.feature.account.presentation

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.feature.account.domain.AccountItem
import dk.rosswap.mobile.feature.account.domain.toAccountItem
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor( // constructor di
    private val sessionManager: SessionManager,
    private val storage: FirebaseStorage,
    private val itemsRepository: ItemsRepository
) : ViewModel() {

    // get live data for photoUrl, posts and user data
    private val _photoUrl = MutableLiveData<String?>()
    val photoUrl: LiveData<String?> = _photoUrl
    private val _posts = MutableLiveData<List<AccountItem>>(emptyList())
    val posts: LiveData<List<AccountItem>> = _posts
    val user
        get() = (sessionManager.authState.value as? AuthState.Authenticated)?.user

    init {
        // observe auth state so we update UI when the user becomes available
        viewModelScope.launch {
            sessionManager.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        _photoUrl.postValue(state.user.photoURL)
                        // Load posts
                        loadMyPosts()
                    }
                    is AuthState.Unauthenticated -> {
                        _photoUrl.postValue(null)
                        _posts.postValue(emptyList())
                    }
                    else -> {
                        // Loading or Error - do nothing
                    }
                }
            }
        }
    }

    fun loadMyPosts(limit: Long = 50) {
        val uid = sessionManager.currentUserId() ?: run {
            _posts.postValue(emptyList())
            return
        }

        viewModelScope.launch {
            val result = try {
                itemsRepository.getLatestItems(limit)
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

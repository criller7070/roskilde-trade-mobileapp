package dk.rosswap.mobile.feature.account.presentation

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.feature.account.domain.AccountItem
import dk.rosswap.mobile.feature.account.domain.toAccountItem
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import dk.rosswap.mobile.feature.items.domain.DeleteItemUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.lang.Exception

@HiltViewModel
class ProfileViewModel @Inject constructor( // constructor di
    private val sessionManager: SessionManager,
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore,
    private val itemsRepository: ItemsRepository,
    private val deleteItemUseCase: DeleteItemUseCase,
    private val exportAccountUseCase: dk.rosswap.mobile.feature.account.domain.ExportAccountUseCase,
    private val deleteAccountUseCase: dk.rosswap.mobile.feature.account.domain.DeleteAccountUseCase
) : ViewModel() {

    // get live data for photoUrl, posts and user data
    private val _photoUrl = MutableLiveData<String?>()
    val photoUrl: LiveData<String?> = _photoUrl
    private val _posts = MutableLiveData<List<AccountItem>>(emptyList())
    val posts: LiveData<List<AccountItem>> = _posts
    private val _uploadError = MutableLiveData<String?>()
    val uploadError: LiveData<String?> = _uploadError
    private val _isUploadingProfilePicture = MutableLiveData(false)
    val isUploadingProfilePicture: LiveData<Boolean> = _isUploadingProfilePicture
    val user
        get() = (sessionManager.authState.value as? AuthState.Authenticated)?.user

    // exporting state
    private val _isExporting = MutableLiveData(false)
    val isExporting: LiveData<Boolean> = _isExporting

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

        _isUploadingProfilePicture.postValue(true)
        _uploadError.postValue(null)

        val ref = storage.reference.child("profilePictures/$uid")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val photoUrlString = downloadUri.toString()
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    photoUri = downloadUri
                }

                viewModelScope.launch {
                    try {
                        // 1. Update Firebase Auth profile
                        val result = sessionManager.updateProfile(profileUpdates)
                        if (result.isSuccess) {
                            // 2. Also update Firestore user document with the new photoURL
                            try {
                                firestore.collection("users").document(uid)
                                    .update("photoURL", photoUrlString)
                                    .await()
                                _photoUrl.postValue(photoUrlString)
                                _isUploadingProfilePicture.postValue(false)
                            } catch (e: Exception) {
                                android.util.Log.e("ProfileViewModel", "Failed to update Firestore with photoURL", e)
                                _uploadError.postValue(e.message ?: "Failed to save profile picture")
                                _isUploadingProfilePicture.postValue(false)
                            }
                        } else {
                            android.util.Log.e("ProfileViewModel", "Failed to update auth profile", result.exceptionOrNull())
                            _uploadError.postValue(result.exceptionOrNull()?.message ?: "Failed to update profile")
                            _isUploadingProfilePicture.postValue(false)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("ProfileViewModel", "Error in uploadProfilePicture", e)
                        _uploadError.postValue(e.message ?: "Upload failed")
                        _isUploadingProfilePicture.postValue(false)
                    }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("ProfileViewModel", "Failed to upload profile picture to storage", e)
                _uploadError.postValue(e.message ?: "Failed to upload profile picture")
                _isUploadingProfilePicture.postValue(false)
            }
    }

    fun deleteAccount(
        onSuccess: () -> Unit,
        onReauthRequired: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        viewModelScope.launch {
            val uid = sessionManager.currentUserId() ?: return@launch

            val result = try {
                deleteAccountUseCase(uid)
            } catch (e: Exception) {
                Result.failure(e)
            }

            result.fold(
                onSuccess = {
                    sessionManager.signOut()
                    onSuccess()
                },
                onFailure = { e ->
                    if (e.message?.contains("401") == true || e is com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                        onReauthRequired()
                    } else {
                        onError(e as Exception)
                    }
                }
            )
        }
    }

    fun deletePost(itemId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        viewModelScope.launch {
            try {
                val result = deleteItemUseCase(itemId)
                result.fold(
                    onSuccess = {
                        // refresh posts
                        loadMyPosts()
                        onSuccess()
                    },
                    onFailure = { e -> onError(e as Exception) }
                )
            } catch (e: Exception) {
                onError(e)
            }
        }
    }

    fun exportAccount(targetUserId: String, onComplete: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val currentUid = sessionManager.currentUserId()
            if (currentUid == null) {
                onComplete(Result.failure(IllegalStateException("Not signed in")))
                return@launch
            }

            if (currentUid != targetUserId) {
                onComplete(Result.failure(SecurityException("Not authorized")))
                return@launch
            }

            _isExporting.postValue(true)
            val res = try {
                exportAccountUseCase(targetUserId)
            } catch (e: Exception) {
                Result.failure<String>(e)
            }
            _isExporting.postValue(false)

            onComplete(res)
        }
    }
}

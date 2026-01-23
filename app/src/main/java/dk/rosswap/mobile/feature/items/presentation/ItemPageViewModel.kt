package dk.rosswap.mobile.feature.items.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ItemPageViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _authorPhotoUrl = MutableStateFlow<String?>(null)
    val authorPhotoUrl: StateFlow<String?> = _authorPhotoUrl.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadAuthorAvatar(userId: String) {
        if (userId.isBlank()) return
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val document = firestore.collection("users").document(userId).get().await()
                val photoUrl = document.getString("photoURL")
                _authorPhotoUrl.value = photoUrl
                _error.value = null
            } catch (e: Exception) {
                _authorPhotoUrl.value = null
                _error.value = e.message ?: "Failed to load author avatar"
            } finally {
                _isLoading.value = false
            }
        }
    }
}

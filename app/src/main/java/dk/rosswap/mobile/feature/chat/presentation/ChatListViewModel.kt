package dk.rosswap.mobile.feature.chat.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import dk.rosswap.mobile.feature.chat.domain.UserChat
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _chats = MutableLiveData<List<UserChat>>(emptyList())
    val chats: LiveData<List<UserChat>> = _chats

    private val _error = MutableLiveData<Throwable?>(null)
    val error: LiveData<Throwable?> = _error

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private var observeJob: Job? = null

    init {
        startObserving()
    }

    fun startObserving() {
        val userId = auth.currentUser?.uid
        if (userId.isNullOrBlank()) {
            _chats.postValue(emptyList())
            _isLoading.postValue(false)
            return
        }

        _isLoading.postValue(true)
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            chatRepository
                .observeChatList(userId)
                .catch { e ->
                    _error.postValue(e)
                    _isLoading.postValue(false)
                }
                .collectLatest { list ->
                    _chats.postValue(list)
                    _isLoading.postValue(false)
                }
        }
    }
}

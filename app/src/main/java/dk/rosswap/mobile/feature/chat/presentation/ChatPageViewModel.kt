package dk.rosswap.mobile.feature.chat.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.feature.chat.domain.ChatMessage
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatPageViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _error = MutableLiveData<Throwable?>(null)
    val error: LiveData<Throwable?> = _error

    private var observeJob: Job? = null

    fun currentUserId(): String? = auth.currentUser?.uid

    fun startObserving(chatId: String) {
        if (chatId.isBlank()) return

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            chatRepository
                .observeMessages(chatId)
                .catch { e -> _error.postValue(e) }
                .collectLatest { list ->
                    _messages.postValue(list)
                }
        }
    }
}

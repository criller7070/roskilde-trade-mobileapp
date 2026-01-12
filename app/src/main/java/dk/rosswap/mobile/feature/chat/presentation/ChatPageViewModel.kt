package dk.rosswap.mobile.feature.chat.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.feature.chat.domain.ChatMessage
import dk.rosswap.mobile.feature.chat.domain.MarkAsReadUseCase
import dk.rosswap.mobile.feature.chat.domain.ObserveMessagesUseCase
import dk.rosswap.mobile.feature.chat.domain.SendMessageUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatPageViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _error = MutableLiveData<Throwable?>(null)
    val error: LiveData<Throwable?> = _error

    private val _isSending = MutableLiveData(false)
    val isSending: LiveData<Boolean> = _isSending

    private var observeJob: Job? = null

    fun currentUserId(): String? = auth.currentUser?.uid

    fun startObserving(chatId: String) {
        if (chatId.isBlank()) return

        val uid = auth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            viewModelScope.launch {
                runCatching { markAsReadUseCase(uid, chatId) }
            }
        }

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeMessagesUseCase(chatId)
                .catch { e -> _error.postValue(e) }
                .collectLatest { list ->
                    _messages.postValue(list)
                }
        }
    }

    fun sendMessage(chatId: String, text: String, onSent: (() -> Unit)? = null) {
        val senderId = auth.currentUser?.uid ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (_isSending.value == true) return

        _isSending.postValue(true)
        viewModelScope.launch {
            try {
                sendMessageUseCase(chatId, senderId, trimmed)
                onSent?.invoke()
            } catch (e: Exception) {
                _error.postValue(e)
            } finally {
                _isSending.postValue(false)
            }
        }
    }
}

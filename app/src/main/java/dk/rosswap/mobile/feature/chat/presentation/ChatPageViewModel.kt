package dk.rosswap.mobile.feature.chat.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.feature.chat.domain.ChatMessage
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import dk.rosswap.mobile.feature.chat.domain.SendMessageUseCase
import dk.rosswap.mobile.feature.chat.domain.UploadChatImageUseCase
import dk.rosswap.mobile.feature.chat.domain.MarkAsReadUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatPageViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val sendMessageUseCase: SendMessageUseCase,
    private val uploadChatImageUseCase: UploadChatImageUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    private val _error = MutableLiveData<Throwable?>(null)
    val error: LiveData<Throwable?> = _error

    private val _rateLimitError = MutableLiveData<String?>(null)
    val rateLimitError: LiveData<String?> = _rateLimitError

    private val _imageUploadError = MutableLiveData<String?>(null)
    val imageUploadError: LiveData<String?> = _imageUploadError

    private val _isSending = MutableLiveData(false)
    val isSending: LiveData<Boolean> = _isSending

    private val _isUploadingImage = MutableLiveData(false)
    val isUploadingImage: LiveData<Boolean> = _isUploadingImage

    private var observeJob: Job? = null

    fun currentUserId(): String? = sessionManager.currentUserId()

    fun startObserving(chatId: String) {
        if (chatId.isBlank()) return

        val uid = sessionManager.currentUserId()
        if (!uid.isNullOrBlank()) {
            viewModelScope.launch {
                runCatching { markAsReadUseCase(uid, chatId) }
            }
        }

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            chatRepository.observeMessages(chatId)
                .catch { e -> _error.postValue(e) }
                .collectLatest { list ->
                    try {
                        Log.d(TAG, "observeMessages: received ${'$'}{list.size} messages")
                        list.forEach { msg ->
                            Log.d(TAG, "msg id=${'$'}{msg.id} sender=${'$'}{msg.senderId} textLen=${'$'}{msg.text?.length ?: 0} imageUrl=${'$'}{msg.imageUrl}")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Error while logging messages", e)
                    }

                    _messages.postValue(list)
                }
        }
    }

    fun sendMessage(chatId: String, text: String, onSent: (() -> Unit)? = null) {
        val senderId = sessionManager.currentUserId() ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (_isSending.value == true) return

        _isSending.postValue(true)
        _rateLimitError.postValue(null)
        viewModelScope.launch {
            val result = sendMessageUseCase(chatId, senderId, trimmed)
            result.onSuccess {
                onSent?.invoke()
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Failed to send message"
                if (exception is IllegalStateException) {
                    // Rate limit error
                    _rateLimitError.postValue(errorMsg)
                } else {
                    _error.postValue(exception)
                }
            }
            _isSending.postValue(false)
        }
    }

    fun sendImageMessage(chatId: String, fileName: String, imageBytes: ByteArray, onSent: (() -> Unit)? = null) {
        val senderId = sessionManager.currentUserId() ?: return
        if (_isUploadingImage.value == true) return

        _isUploadingImage.postValue(true)
        _imageUploadError.postValue(null)
        viewModelScope.launch {
            val result = uploadChatImageUseCase(chatId, senderId, fileName, imageBytes)
            result.onSuccess {
                onSent?.invoke()
            }.onFailure { exception ->
                val errorMsg = exception.message ?: "Failed to upload image"
                _imageUploadError.postValue(errorMsg)
            }
            _isUploadingImage.postValue(false)
        }
    }

    companion object {
        private const val TAG = "ChatPageViewModel"
    }
}

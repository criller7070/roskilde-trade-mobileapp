package dk.rosswap.mobile.feature.chat.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatPageViewModel @Inject constructor(
    // We'll inject ChatRepository here once we implement message observation.
) : ViewModel() {

    private val _messages = MutableLiveData<List<String>>(emptyList())
    val messages: LiveData<List<String>> = _messages

    fun setPlaceholder(messages: List<String>) {
        _messages.postValue(messages)
    }
}

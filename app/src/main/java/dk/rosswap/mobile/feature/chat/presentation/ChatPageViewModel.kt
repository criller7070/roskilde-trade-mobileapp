package dk.rosswap.mobile.feature.chat.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatPageViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<String>>().apply {
        value = listOf("Hi!", "Hello, how are you?", "Good, thanks.")
    }
    val messages: LiveData<List<String>> = _messages
}

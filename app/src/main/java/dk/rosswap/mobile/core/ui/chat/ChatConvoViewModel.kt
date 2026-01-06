package dk.rosswap.mobile.core.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatConvoViewModel : ViewModel() {

    private val _messages = MutableLiveData<List<String>>().apply {
        value = listOf("Hi!", "Hello, how are you?", "Good, thanks.")
    }
    val messages: LiveData<List<String>> = _messages
}

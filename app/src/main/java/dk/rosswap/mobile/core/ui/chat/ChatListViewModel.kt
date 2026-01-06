package dk.rosswap.mobile.core.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ChatListViewModel : ViewModel() {

    private val _chats = MutableLiveData<List<String>>().apply {
        value = listOf("Alice", "Bob", "Carol")
    }
    val chats: LiveData<List<String>> = _chats
}

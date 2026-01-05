package dk.rosswap.mobile.core.ui.components.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is your mom"
    }
    val text: LiveData<String> = _text
}


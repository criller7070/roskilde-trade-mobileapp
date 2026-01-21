package dk.rosswap.mobile.feature.home.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel   // ⬅️ THIS WAS MISSING

// ViewModel added for consistency, Home is basically static so it's not needed

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Placeholder"
    }

    val text: LiveData<String> = _text
}

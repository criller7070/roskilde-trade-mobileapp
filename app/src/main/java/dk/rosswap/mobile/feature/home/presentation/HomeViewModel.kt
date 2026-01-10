package dk.rosswap.mobile.feature.home.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel   // ⬅️ THIS WAS MISSING

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is your mom"
    }

    val text: LiveData<String> = _text
}

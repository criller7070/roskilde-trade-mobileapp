package dk.rosswap.mobile.feature.items.presentation

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.feature.items.domain.AddItemUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddItemViewModel @Inject constructor(
    private val addItemUseCase: AddItemUseCase
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _createResult = MutableLiveData<Result<Unit>>()
    val createResult: LiveData<Result<Unit>> = _createResult

    fun createPost(title: String, description: String, imageUri: Uri?, type: String) {
        if (_isLoading.value == true) return

        _isLoading.postValue(true)
        viewModelScope.launch {
            val result = addItemUseCase(
                title = title,
                description = description,
                imageUri = imageUri,
                type = type
            )
            _createResult.postValue(result)
            _isLoading.postValue(false)
        }
    }
}

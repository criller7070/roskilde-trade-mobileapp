package dk.rosswap.mobile.feature.liked.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.Item
import dk.rosswap.mobile.feature.liked.domain.GetDislikedItemsUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DislikedViewModel @Inject constructor(
    private val getDislikedItemsUseCase: GetDislikedItemsUseCase
) : ViewModel() {

    private val _dislikedItems = MutableLiveData<List<Item>>(emptyList())
    val dislikedItems: LiveData<List<Item>> = _dislikedItems

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun refresh() {
        _isLoading.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            val result = getDislikedItemsUseCase()
            if (result.isSuccess) {
                _dislikedItems.postValue(result.getOrDefault(emptyList()))
            } else {
                _errorMessage.postValue(result.exceptionOrNull()?.message ?: "Error loading disliked items")
            }
            _isLoading.postValue(false)
        }
    }

    init {
        refresh()
    }
}

package dk.rosswap.mobile.feature.items.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.feature.items.domain.GetItemsUseCase
import dk.rosswap.mobile.feature.liked.domain.LikeItemUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemListViewModel @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase,
    private val likeItemUseCase: LikeItemUseCase
) : ViewModel() {

    private val _items = MutableLiveData<List<Item>>(emptyList())
    val items: LiveData<List<Item>> = _items

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun refresh(limit: Long = 50) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = getItemsUseCase(limit)
            if (result.isSuccess) {
                _items.postValue(result.getOrDefault(emptyList()))
            } else {
                _errorMessage.postValue(result.exceptionOrNull()?.message ?: "Failed to load items")
            }
            _isLoading.postValue(false)
        }
    }

    fun likeItem(item: Item) {
        viewModelScope.launch {
            val result = likeItemUseCase(item.id)
            if (result.isFailure) {
                _errorMessage.postValue("Failed to like item: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    init {
        refresh()
    }
}

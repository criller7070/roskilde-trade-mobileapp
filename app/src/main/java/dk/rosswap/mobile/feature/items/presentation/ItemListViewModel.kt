package dk.rosswap.mobile.feature.items.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.Item
import dk.rosswap.mobile.feature.items.domain.GetItemsUseCase
import dk.rosswap.mobile.feature.liked.domain.DislikeItemUseCase
import dk.rosswap.mobile.feature.liked.domain.LikeItemUseCase
import dk.rosswap.mobile.feature.liked.domain.RemoveDislikeItemUseCase
import dk.rosswap.mobile.feature.liked.domain.UnlikeItemUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemListViewModel @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase,
    private val likeItemUseCase: LikeItemUseCase,
    private val dislikeItemUseCase: DislikeItemUseCase,
    private val unlikeItemUseCase: UnlikeItemUseCase,
    private val removeDislikeItemUseCase: RemoveDislikeItemUseCase
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
            // First, remove from disliked list if it exists there
            val removeDislikeResult = removeDislikeItemUseCase(item.id)
            if (removeDislikeResult.isFailure) {
                // Log but continue - item might not be in disliked list
            }
            
            // Then add to liked list
            val result = likeItemUseCase(item.id)
            if (result.isFailure) {
                _errorMessage.postValue("Failed to like item: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun dislikeItem(item: Item) {
        viewModelScope.launch {
            // First, remove from liked list if it exists there
            val unlikeResult = unlikeItemUseCase(item.id)
            if (unlikeResult.isFailure) {
                // Log but continue - item might not be in liked list
            }
            
            // Then add to disliked list
            val result = dislikeItemUseCase(item.id)
            if (result.isFailure) {
                _errorMessage.postValue("Failed to dislike item: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    init {
        refresh()
    }
}

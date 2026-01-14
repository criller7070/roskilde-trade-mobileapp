package dk.rosswap.mobile.feature.liked.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.Item
import dk.rosswap.mobile.feature.liked.domain.GetDislikedItemsUseCase
import dk.rosswap.mobile.feature.liked.domain.LikeItemUseCase
import dk.rosswap.mobile.feature.liked.domain.RemoveDislikeItemUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DislikedViewModel @Inject constructor(
    private val getDislikedItemsUseCase: GetDislikedItemsUseCase,
    private val removeDislikeItemUseCase: RemoveDislikeItemUseCase,
    private val likeItemUseCase: LikeItemUseCase
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

    fun likeAgain(item: Item) {
        // Optimistic update: remove from UI immediately
        val currentList = _dislikedItems.value.orEmpty().toMutableList()
        currentList.remove(item)
        _dislikedItems.value = currentList

        viewModelScope.launch {
            // 1. Remove from disliked
            val removeResult = removeDislikeItemUseCase(item.id)
            if (removeResult.isSuccess) {
                // 2. Add to liked
                val likeResult = likeItemUseCase(item.id)
                if (likeResult.isFailure) {
                    _errorMessage.postValue("Failed to add to liked: ${likeResult.exceptionOrNull()?.message}")
                    // Rollback UI? For simplicity, we assume success or refresh later.
                }
            } else {
                _errorMessage.postValue("Failed to remove from disliked: ${removeResult.exceptionOrNull()?.message}")
                // Rollback UI
                val rolledBackList = _dislikedItems.value.orEmpty().toMutableList()
                rolledBackList.add(item)
                _dislikedItems.postValue(rolledBackList)
            }
        }
    }

    init {
        refresh()
    }
}

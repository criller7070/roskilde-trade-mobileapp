package dk.rosswap.mobile.feature.items.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import dk.rosswap.mobile.feature.liked.domain.DislikeItemUseCase
import dk.rosswap.mobile.feature.liked.domain.LikeItemUseCase
import dk.rosswap.mobile.feature.liked.domain.UnDislikeItemUseCase
import dk.rosswap.mobile.feature.liked.domain.UnlikeItemUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemListViewModel @Inject constructor(
    private val itemsRepository: ItemsRepository,
    private val likeItemUseCase: LikeItemUseCase,
    private val dislikeItemUseCase: DislikeItemUseCase,
    private val unlikeItemUseCase: UnlikeItemUseCase,
    private val unDislikeItemUseCase: UnDislikeItemUseCase
) : ViewModel() {

    private val _items = MutableLiveData<List<Item>>(emptyList())
    val items: LiveData<List<Item>> = _items

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // Track liked items to persist state across screen navigation
    private val _likedItemIds = MutableLiveData<Set<String>>(emptySet())
    val likedItemIds: LiveData<Set<String>> = _likedItemIds

    fun refresh(limit: Long = 50) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = try {
                itemsRepository.getLatestItems(limit)
            } catch (e: Exception) {
                Result.failure<List<Item>>(e)
            }

            if (result.isSuccess) {
                _items.postValue(result.getOrDefault(emptyList()))
            } else {
                _errorMessage.postValue(result.exceptionOrNull()?.message ?: "Failed to load items")
            }
            _isLoading.postValue(false)
        }
    }

    fun toggleLike(itemId: String) {
        val current = _likedItemIds.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _likedItemIds.value = current
    }

    fun isLiked(itemId: String): Boolean {
        return _likedItemIds.value?.contains(itemId) ?: false
    }

    fun likeItem(item: Item) {
        viewModelScope.launch {
            // Update UI state immediately
            toggleLike(item.id)
            
            // First, remove from disliked list if it exists there
            // Silently continue if removal fails - item might not be in disliked list
            unDislikeItemUseCase(item.id)
            
            // Then add to liked list
            val result = likeItemUseCase(item.id)
            if (result.isFailure) {
                // Revert UI state on failure
                toggleLike(item.id)
                _errorMessage.postValue("Failed to like item: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun dislikeItem(item: Item) {
        viewModelScope.launch {
            // Remove from liked UI state if present
            val current = _likedItemIds.value?.toMutableSet() ?: mutableSetOf()
            if (current.contains(item.id)) {
                current.remove(item.id)
                _likedItemIds.value = current
            }
            
            // First, remove from liked list if it exists there
            // Silently continue if removal fails - item might not be in liked list
            unlikeItemUseCase(item.id)
            
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

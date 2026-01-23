package dk.rosswap.mobile.feature.items.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import dk.rosswap.mobile.feature.liked.domain.SwipeUseCase
import dk.rosswap.mobile.feature.liked.domain.LikedRepository
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.common.AuthState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ItemListViewModel @Inject constructor(
    private val itemsRepository: ItemsRepository,
    private val swipeUseCase: SwipeUseCase,
    private val likedRepository: LikedRepository,
    private val sessionManager: SessionManager
) : ViewModel() {
    private val _items = MutableLiveData<List<Item>>(emptyList())
    val items: LiveData<List<Item>> = _items
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage
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

    // do like/dislike
    fun toggleLike(itemId: String) {
        val current = _likedItemIds.value?.toMutableSet() ?: mutableSetOf()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _likedItemIds.value = current
    }
    // check if item is liked
    fun isLiked(itemId: String): Boolean {
        return _likedItemIds.value?.contains(itemId) ?: false
    }

    // do like
    fun likeItem(item: Item) {
        viewModelScope.launch {
            // Update UI state immediately
            toggleLike(item.id)

            // Use SwipeUseCase to perform like & cleanup operations
            val result = swipeUseCase.swipeRight(item.id)
            if (result.isFailure) {
                // Revert UI state on failure
                toggleLike(item.id)
                _errorMessage.postValue("Failed to like item: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    // do dislike
    fun dislikeItem(item: Item) {
        viewModelScope.launch {
            // Remove from liked UI state if present
            val current = _likedItemIds.value?.toMutableSet() ?: mutableSetOf()
            if (current.contains(item.id)) {
                current.remove(item.id)
                _likedItemIds.value = current
            }

            val result = swipeUseCase.swipeLeft(item.id)
            if (result.isFailure) {
                _errorMessage.postValue("Failed to dislike item: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    init {
        refresh()
        observeLikedIds()
    }

    private fun observeLikedIds() {
        // observe session auth state and subscribe to liked items for the signed-in user.
        viewModelScope.launch {
            sessionManager.authState.collectLatest { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        val uid = state.user.uid
                        try {
                            likedRepository.getLikedItems(uid).collect { likedItems ->
                                val ids = likedItems.mapNotNull { it.item.id }.toSet()
                                _likedItemIds.postValue(ids)
                            }
                        } catch (e: Exception) {
                            _likedItemIds.postValue(emptySet())
                        }
                    }
                    else -> {
                        // Not authenticated - clear liked ids
                        _likedItemIds.postValue(emptySet())
                    }
                }
            }
        }
    }
}

package dk.rosswap.mobile.feature.items.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.feature.liked.domain.SwipeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SwipeViewModel @Inject constructor(
    private val itemsRepository: ItemsRepository,
    private val sessionManager: SessionManager,
    private val swipeUseCase: SwipeUseCase
) : ViewModel() {

    private val _posts = MutableStateFlow<List<Item>>(emptyList())
    val posts: StateFlow<List<Item>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Track locally swiped IDs so we can filter them out until repository pushes updates
    private val locallySwipedIds = mutableSetOf<String>()

    fun start(initialLimit: Long = 50) {
        // Trigger initial load
        refresh(initialLimit)
    }

    fun refresh(limit: Long = 50) {
        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val result = try {
                itemsRepository.getLatestItems(limit)
            } catch (e: Exception) {
                Result.failure<List<Item>>(e)
            }

            if (result.isSuccess) {
                val items = result.getOrDefault(emptyList())
                // Filter out locally swiped items and own items
                val currentUserId = sessionManager.currentUserId()
                val filtered = items.filter { it.id !in locallySwipedIds && it.userId != currentUserId }
                _posts.value = filtered
                _error.value = null
            } else {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to load items"
            }
            _isLoading.value = false
        }
    }

    fun registerLocalSwipe(itemId: String) {
        locallySwipedIds.add(itemId)
        // Optimistically remove from posts
        _posts.update { list -> list.filterNot { it.id == itemId } }
    }

    fun like(item: Item) {
        val itemId = item.id
        registerLocalSwipe(itemId)
        viewModelScope.launch(Dispatchers.IO) {
            val result = swipeUseCase.swipeRight(itemId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to like item"
                // On failure, remove from locallySwiped to allow reappearance
                locallySwipedIds.remove(itemId)
                refresh()
            }
        }
    }

    fun dislike(item: Item) {
        val itemId = item.id
        registerLocalSwipe(itemId)
        viewModelScope.launch(Dispatchers.IO) {
            val result = swipeUseCase.swipeLeft(itemId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to dislike item"
                locallySwipedIds.remove(itemId)
                refresh()
            }
        }
    }

    // Optionally expose a method to clear errors
    fun clearError() {
        _error.value = null
    }

    // Expose current user id in a controlled way for navigation actions
    fun currentUserId(): String? = sessionManager.currentUserId()
}

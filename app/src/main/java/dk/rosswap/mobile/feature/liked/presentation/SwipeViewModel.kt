package dk.rosswap.mobile.feature.liked.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import dk.rosswap.mobile.feature.liked.domain.SwipeUseCase
import dk.rosswap.mobile.feature.liked.domain.LikedRepository
import dk.rosswap.mobile.feature.liked.domain.DislikedRepository
import kotlinx.coroutines.flow.first
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
    private val swipeUseCase: SwipeUseCase,
    private val likedRepository: LikedRepository,
    private val dislikedRepository: DislikedRepository
) : ViewModel() {
    private val _posts = MutableStateFlow<List<Item>>(emptyList())
    val posts: StateFlow<List<Item>> = _posts.asStateFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val locallySwipedIds = mutableSetOf<String>()

    fun start(initialLimit: Long = 50) {
        refresh(initialLimit)
    }

    // refresh posts from the server. There's gotta be a more efficient way
    // to do this but this works for now.
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
                // filter out locally swiped items and own items
                val currentUserId = sessionManager.currentUserId()

                // ... filter out liked ids...
                val likedIds: Set<String> = if (currentUserId != null) {
                    try { // this fitlers out already-swiped items
                        likedRepository.getLikedItems(currentUserId).first()
                            .mapNotNull { it.item.id }
                            .filter { it.isNotBlank() }
                            .toSet()
                    } catch (e: Exception) {
                        emptySet()
                    }
                } else emptySet()

                // ... filter out disliked ids...
                val dislikedIds: Set<String> = if (currentUserId != null) {
                    try {
                        val dislikedRes = dislikedRepository.getDislikedItems(currentUserId)
                        if (dislikedRes.isSuccess) {
                            dislikedRes.getOrNull()?.map { it.item.id }
                                ?.filter { it.isNotBlank() }
                                ?.toSet() ?: emptySet()
                        } else emptySet()
                    } catch (_: Exception) {
                        emptySet()
                    }
                } else emptySet()

                val filtered = items.filter { it.id !in locallySwipedIds && it.userId != currentUserId }
                    .filter { it.id !in likedIds }
                    .filter { it.id !in dislikedIds }

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

    // call like when swiping right
    fun like(item: Item) {
        val itemId = item.id
        registerLocalSwipe(itemId)
        viewModelScope.launch(Dispatchers.IO) {
            val  result = swipeUseCase.swipeRight(itemId)
            if (result.isFailure) {
                _error.value = result.exceptionOrNull()?.message ?: "Failed to like item"
                locallySwipedIds.remove(itemId)
                refresh()
            }
        }
    }

    // call dislike when swiping left
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

    // optionally expose a method to clear errors
    fun clearError() {
        _error.value = null
    }

    // expose current user id in a controlled way for navigation actions
    fun currentUserId(): String? = sessionManager.currentUserId()
}
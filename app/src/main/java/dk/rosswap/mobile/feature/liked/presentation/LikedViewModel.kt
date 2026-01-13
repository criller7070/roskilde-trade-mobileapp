package dk.rosswap.mobile.feature.liked.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.feature.liked.domain.GetLikedItemsUseCase
import dk.rosswap.mobile.feature.liked.domain.UnlikeItemUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikedViewModel @Inject constructor(
    private val getLikedItemsUseCase: GetLikedItemsUseCase,
    private val unlikeItemUseCase: UnlikeItemUseCase
) : ViewModel() {

    private val _likedPosts = MutableLiveData<List<Item>>()
    val likedPosts: LiveData<List<Item>> = _likedPosts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    companion object {
        private const val TAG = "LikedViewModel"
    }

    init {
        loadLikedPosts()
    }

    fun loadLikedPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = getLikedItemsUseCase()
            if (result.isSuccess) {
                val items = result.getOrDefault(emptyList())
                Log.d(TAG, "Loaded ${items.size} liked items")
                _likedPosts.value = items
            } else {
                Log.e(TAG, "Failed to load liked posts", result.exceptionOrNull())
                _likedPosts.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun unlikePost(item: Item) {
        viewModelScope.launch {
            // Optimistic update
            val currentList = _likedPosts.value.orEmpty().toMutableList()
            currentList.removeAll { it.id == item.id }
            _likedPosts.value = currentList

            val result = unlikeItemUseCase(item.id)
            if (result.isFailure) {
                Log.e(TAG, "Failed to unlike post", result.exceptionOrNull())
                loadLikedPosts() // Revert
            }
        }
    }
}

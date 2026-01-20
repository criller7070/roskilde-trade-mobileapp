package dk.rosswap.mobile.feature.liked.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.feature.liked.domain.GetLikedItemsUseCase
import dk.rosswap.mobile.feature.liked.domain.LikedItem
import dk.rosswap.mobile.feature.liked.domain.UnlikeItemUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LikedViewModel @Inject constructor(
    private val getLikedItemsUseCase: GetLikedItemsUseCase,
    private val unlikeItemUseCase: UnlikeItemUseCase
) : ViewModel() {

    private val _likedPosts = MutableLiveData<List<LikedItem>>()
    val likedPosts: LiveData<List<LikedItem>> = _likedPosts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    companion object {
        private const val TAG = "LikedViewModel"
    }

    init {
        observeLikedPosts()
    }

    private fun observeLikedPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            getLikedItemsUseCase()
                .collect { items ->
                    Log.d(TAG, "Loaded ${items.size} liked items")
                    _likedPosts.value = items
                    _isLoading.value = false
                }
        }
    }

    fun unlikePost(likedItem: LikedItem) {
        viewModelScope.launch {
            val result = unlikeItemUseCase(likedItem.item.id)
            if (result.isFailure) {
                Log.e(TAG, "Failed to unlike post", result.exceptionOrNull())
                // No need to revert manually as the flow will emit the current state from server
                // but we might want to show an error to the user
            }
        }
    }
}

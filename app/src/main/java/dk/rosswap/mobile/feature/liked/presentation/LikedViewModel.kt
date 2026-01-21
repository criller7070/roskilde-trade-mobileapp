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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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
        private const val MAX_RETRIES = 3L
        private const val INITIAL_DELAY_SECONDS = 1L
    }

    init {
        observeLikedPosts()
    }

    private fun observeLikedPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            getLikedItemsUseCase()
                .retryWhen { cause, attempt ->
                    if (attempt < MAX_RETRIES) {
                        // Calculate exponential backoff: 1s, 2s, 4s (using bit shift: 1 << 0 = 1, 1 << 1 = 2, 1 << 2 = 4)
                        val delayTime = INITIAL_DELAY_SECONDS * (1L shl attempt.toInt())
                        Log.w(TAG, "Error in liked items flow (attempt ${attempt + 1}/$MAX_RETRIES), retrying in ${delayTime}s", cause)
                        delay(delayTime.seconds)
                        true
                    } else {
                        Log.e(TAG, "Error in liked items flow after $MAX_RETRIES retries, giving up", cause)
                        false
                    }
                }
                .catch { e ->
                    // This catch block is only reached if retries are exhausted
                    Log.e(TAG, "Failed to observe liked items after retries", e)
                    _isLoading.value = false
                    _likedPosts.value = emptyList()
                }
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
            }
        }
    }
}

package dk.rosswap.mobile.feature.liked.presentation

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.feature.items.domain.Item
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class LikedViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
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
        val userId = auth.currentUser?.uid
        if (userId == null) {
            _likedPosts.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Get the list of liked IDs from the user profile
                val userDoc = firestore.collection("users").document(userId).get().await()
                
                // Use generic get and safe cast
                val rawLiked = userDoc.get("likedItemIds")
                val likedIds = (rawLiked as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

                Log.d(TAG, "Found ${likedIds.size} liked item IDs")

                if (likedIds.isEmpty()) {
                    _likedPosts.value = emptyList()
                    return@launch
                }

                // 2. Fetch the actual items for these IDs
                // Firestore "in" queries are limited to 10 items, so we chunk requests
                val items = mutableListOf<Item>()
                val chunks = likedIds.chunked(10)

                for (chunk in chunks) {
                    val snapshot = firestore.collection("items")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()

                    val chunkItems = snapshot.documents.mapNotNull { doc ->
                        // Manually map fields to ensure safety with the new Item definition
                        val title = doc.getString("title") ?: return@mapNotNull null
                        val description = doc.getString("description") ?: ""
                        val mode = doc.getString("mode") ?: "bytte"
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        val userIdStr = doc.getString("userId") ?: ""
                        val userName = doc.getString("userName") ?: ""
                        val createdAt = doc.getTimestamp("createdAt")
                        val price = doc.getDouble("price") ?: 0.0

                        Item(
                            id = doc.id,
                            title = title,
                            description = description,
                            mode = mode,
                            imageUrl = imageUrl,
                            userId = userIdStr,
                            userName = userName,
                            createdAt = createdAt,
                            price = price
                        )
                    }
                    items.addAll(chunkItems)
                }

                Log.d(TAG, "Loaded ${items.size} liked items")
                _likedPosts.value = items

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load liked posts", e)
                // Optionally show error state
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun unlikePost(item: Item) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // Remove from local list immediately for UI responsiveness
                val currentList = _likedPosts.value.orEmpty().toMutableList()
                currentList.removeAll { it.id == item.id }
                _likedPosts.value = currentList

                // Remove from Firestore
                firestore.collection("users").document(userId)
                    .update("likedItemIds", FieldValue.arrayRemove(item.id))
                    .await()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unlike post", e)
                loadLikedPosts() // Revert/Refresh on error
            }
        }
    }
}

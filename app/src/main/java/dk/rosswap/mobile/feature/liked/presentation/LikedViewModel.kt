package dk.rosswap.mobile.feature.liked.presentation

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

    init {
        loadLikedPosts()
    }

    fun loadLikedPosts() {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 1. Get the list of liked IDs from the user profile
                val userDoc = firestore.collection("users").document(userId).get().await()
                val likedIds = userDoc.get("likedItemIds") as? List<String> ?: emptyList()

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

                    val chunkItems = snapshot.toObjects(Item::class.java).mapIndexed { index, item ->
                        // Ensure ID is set (snapshot.toObjects might not set document ID automatically)
                        item.copy(id = snapshot.documents[index].id)
                    }
                    items.addAll(chunkItems)
                }

                _likedPosts.value = items

            } catch (e: Exception) {
                // Handle error (e.g., show toast or empty state)
                e.printStackTrace()
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
                // If it fails, reload original list
                loadLikedPosts()
            }
        }
    }
}

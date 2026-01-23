package dk.rosswap.mobile.feature.items.domain

import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeleteItemUseCase @Inject constructor(
    private val itemsRepository: ItemsRepository,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(itemId: String): Result<Unit> {
        // 1. ensure user is logged in
        val uid = sessionManager.currentUserId()
            ?: return Result.failure(IllegalStateException("You must be logged in to delete an item."))

        return try {
            // 2. get item doc to verify ownership lest we delete it even if we don't own it
            val doc = firestore.collection("items").document(itemId).get().await()
            if (!doc.exists()) {
                return Result.failure(IllegalArgumentException("Item not found"))
            }

            val ownerId = doc.getString("userId") ?: ""
            if (ownerId != uid) {
                return Result.failure(SecurityException("You can only delete your own items"))
            }

            // 3. delegate deletion to repository (which will remove storage image and doc)
            itemsRepository.deleteItem(itemId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

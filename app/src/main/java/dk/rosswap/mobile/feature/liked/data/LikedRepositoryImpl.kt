package dk.rosswap.mobile.feature.liked.data

import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.liked.domain.LikedMapper
import dk.rosswap.mobile.feature.liked.domain.LikedRepository
import dk.rosswap.mobile.feature.liked.data.LikedDto
import dk.rosswap.mobile.feature.liked.domain.LikedItem
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LikedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LikedRepository {

    companion object {
        private const val TAG = "LikedRepositoryImpl"
    }

    override suspend fun getLikedItems(userId: String): Result<List<LikedItem>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            // Gracefully handle if doc or field doesn't exist
            val rawLiked = if (userDoc.exists()) userDoc.get("likedItemIds") else null
            val likedDtos = (rawLiked as? List<*>)?.mapNotNull { LikedDto.fromAny(it) } ?: emptyList()
            val likedIds = likedDtos.mapNotNull { it.itemId }

            if (likedIds.isEmpty()) return Result.success(emptyList())
            val likedAtById = likedDtos.mapNotNull { dto -> dto.itemId?.let { it to dto.likedAt } }.toMap()

            val items = mutableListOf<LikedItem>()
            val chunks = likedIds.chunked(10)

            for (chunk in chunks) {
                // Should not happen, but safe check
                if (chunk.isEmpty()) continue

                val snapshot = firestore.collection("items")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                val docById = snapshot.documents.associateBy { it.id }

                for (id in chunk) {
                    val doc = docById[id] ?: continue
                    try {
                        val item = LikedMapper.fromDoc(doc)
                        val likedAt = likedAtById[id]
                        items.add(LikedItem(item = item, likedAt = likedAt))

                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to map liked item doc $id", e)
                    }
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching liked items", e)
            Result.failure(e)
        }
    }
}

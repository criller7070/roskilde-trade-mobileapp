package dk.rosswap.mobile.feature.liked.data

import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.liked.domain.DislikedRepository
import dk.rosswap.mobile.feature.liked.domain.DislikedItem
import dk.rosswap.mobile.feature.liked.domain.DislikedMapper
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DislikedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DislikedRepository {

    companion object {
        private const val TAG = "DislikedRepositoryImpl"
    }

    override suspend fun getDislikedItems(userId: String): Result<List<DislikedItem>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val rawDisliked = if (userDoc.exists()) userDoc.get("dislikedItemIds") else null
            val dislikedDtos = (rawDisliked as? List<*>)?.mapNotNull { DislikedDto.fromAny(it) } ?: emptyList()
            
            // Filter out empty IDs to prevent Firestore crash
            val dislikedIds = dislikedDtos.mapNotNull { it.itemId }
                .filter { it.isNotBlank() }

            if (dislikedIds.isEmpty()) return Result.success(emptyList())
            val dislikedAtById = dislikedDtos.mapNotNull { dto -> dto.itemId?.let { it to dto.dislikedAt } }.toMap()

            val items = mutableListOf<DislikedItem>()
            val chunks = dislikedIds.chunked(10)

            for (chunk in chunks) {
                // Double check chunk is not empty
                if (chunk.isEmpty()) continue

                val snapshot = firestore.collection("items")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                val docById = snapshot.documents.associateBy { it.id }

                for (id in chunk) {
                    val doc = docById[id] ?: continue
                    try {
                        val item = DislikedMapper.fromDoc(doc)
                        val dislikedAt = dislikedAtById[id]
                        items.add(DislikedItem(item = item, dislikedAt = dislikedAt))
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to map disliked item doc $id", e)
                    }
                }
            }
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching disliked items", e)
            Result.failure(e)
        }
    }
}

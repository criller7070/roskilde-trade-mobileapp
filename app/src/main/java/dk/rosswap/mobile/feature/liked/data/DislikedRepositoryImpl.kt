package dk.rosswap.mobile.feature.liked.data

import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.liked.domain.DislikedRepository
import dk.rosswap.mobile.feature.liked.domain.DislikedItem
import dk.rosswap.mobile.feature.liked.domain.DislikedMapper
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Reminder: The way we split Repositories up is:
// - Use-facing functions are staged in UseCase
// - Behind-the-scenes functions are put here (unless too big)
// - CRUD-like operations are done in repos
// All of this is mostly a culture choice; some teams just stage and invoke in UseCase
// and have all the business logic in here. We care about aesthetics and shorter files.

class DislikedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : DislikedRepository {

    companion object {
        private const val TAG = "DislikedRepositoryImpl"
    }

    override suspend fun getDislikedItems(userId: String): Result<List<DislikedItem>> {
        return try {
            // 1. get user docs
            val userDoc = firestore.collection("users").document(userId).get().await()
            val rawDisliked = if (userDoc.exists()) userDoc.get("dislikedItemIds") else null
            val dislikedDtos = (rawDisliked as? List<*>)?.mapNotNull { DislikedDto.fromAny(it) } ?: emptyList()
            val validDislikedDtos = dislikedDtos.filter { !it.itemId.isNullOrBlank() }

            // 2. filter out empty IDs to prevent Firestore crash
            val dislikedIds = validDislikedDtos.mapNotNull { it.itemId }
            if (dislikedIds.isEmpty()) return Result.success(emptyList())
            val dislikedAtById = validDislikedDtos
                .mapNotNull { dto -> dto.itemId?.let { it to dto.dislikedAt } }
                .toMap()
            val items = mutableListOf<DislikedItem>()
            val chunks = dislikedIds.chunked(10)

            // 3. Divide into chunks - basically batch writing. Mostly for efficiency
            for (chunk in chunks) {
                if (chunk.isEmpty()) continue

                // 4. Get items
                val snapshot = firestore.collection("items")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()
                val docById = snapshot.documents.associateBy { it.id }

                // 5. map and get disliked docs
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

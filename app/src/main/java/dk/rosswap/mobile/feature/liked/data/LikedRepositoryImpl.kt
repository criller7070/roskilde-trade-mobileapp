package dk.rosswap.mobile.feature.liked.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.feature.liked.domain.LikedMapper
import dk.rosswap.mobile.feature.liked.domain.LikedRepository
import dk.rosswap.mobile.feature.liked.data.LikedDto
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LikedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : LikedRepository {

    companion object {
        private const val TAG = "LikedRepositoryImpl"
    }

    override suspend fun getLikedItems(userId: String): Result<List<Item>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val rawLiked = userDoc.get("likedItemIds")
            val likedDtos = (rawLiked as? List<*>)?.mapNotNull { LikedDto.fromAny(it) } ?: emptyList()
            val likedIds = likedDtos.mapNotNull { it.itemId }

            if (likedIds.isEmpty()) return Result.success(emptyList())
            val likedAtById = likedDtos.mapNotNull { dto -> dto.itemId?.let { it to dto.likedAt } }.toMap()

            val items = mutableListOf<Item>()
            val chunks = likedIds.chunked(10)

            for (chunk in chunks) {
                val snapshot = firestore.collection("items")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .await()

                // Build a lookup so we can append results in the same order as likedIds
                val docById = snapshot.documents.associateBy { it.id }

                for (id in chunk) {
                    val doc = docById[id] ?: continue
                    try {
                        val item = LikedMapper.fromDoc(doc)
                        items.add(item)

                        // Log likedAt when available (non-breaking enhancement)
                        likedAtById[id]?.let { likedAt ->
                            Log.d(TAG, "Liked item: $id likedAt=$likedAt")
                        }
                    } catch (e: Exception) {
                        // Skip malformed item docs but continue processing
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

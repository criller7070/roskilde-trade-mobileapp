package dk.rosswap.mobile.feature.liked.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.items.domain.Item
import dk.rosswap.mobile.feature.liked.domain.LikedRepository
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
            val likedIds = (rawLiked as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            if (likedIds.isEmpty()) return Result.success(emptyList())

            fetchItemsByIds(likedIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching liked items", e)
            Result.failure(e)
        }
    }

    override suspend fun getDislikedItems(userId: String): Result<List<Item>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val rawDisliked = userDoc.get("dislikedItemIds")
            val dislikedIds = (rawDisliked as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            if (dislikedIds.isEmpty()) return Result.success(emptyList())

            fetchItemsByIds(dislikedIds)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching disliked items", e)
            Result.failure(e)
        }
    }

    private suspend fun fetchItemsByIds(ids: List<String>): Result<List<Item>> {
        val items = mutableListOf<Item>()
        val chunks = ids.chunked(10)

        for (chunk in chunks) {
            val snapshot = firestore.collection("items")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .await()

            val chunkItems = snapshot.documents.mapNotNull { doc ->
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
        return Result.success(items)
    }
}

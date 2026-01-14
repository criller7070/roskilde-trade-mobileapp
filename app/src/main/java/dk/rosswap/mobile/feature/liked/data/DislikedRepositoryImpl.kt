package dk.rosswap.mobile.feature.liked.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.common.Item
import dk.rosswap.mobile.feature.liked.domain.DislikedRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DislikedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : DislikedRepository {

    companion object {
        private const val TAG = "DislikedRepositoryImpl"
    }

    override suspend fun getDislikedItems(userId: String): Result<List<Item>> {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val rawDisliked = userDoc.get("dislikedItemIds")
            val dislikedIds = (rawDisliked as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            if (dislikedIds.isEmpty()) return Result.success(emptyList())

            val items = mutableListOf<Item>()
            // Split into chunks of 10 for 'whereIn' queries
            val chunks = dislikedIds.chunked(10)

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
            Result.success(items)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching disliked items", e)
            Result.failure(e)
        }
    }
}

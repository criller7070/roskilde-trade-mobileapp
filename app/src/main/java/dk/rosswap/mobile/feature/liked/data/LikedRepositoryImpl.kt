package dk.rosswap.mobile.feature.liked.data

import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.liked.domain.LikedMapper
import dk.rosswap.mobile.feature.liked.domain.LikedRepository
import dk.rosswap.mobile.feature.liked.data.LikedDto
import dk.rosswap.mobile.feature.liked.domain.LikedItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LikedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LikedRepository {

    companion object {
        private const val TAG = "LikedRepositoryImpl"
    }

    override fun getLikedItems(userId: String): Flow<List<LikedItem>> = callbackFlow {
        val registration = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val rawLiked = snapshot.get("likedItemIds")
                    val likedDtos = (rawLiked as? List<*>)?.mapNotNull { LikedDto.fromAny(it) } ?: emptyList()
                    trySend(likedDtos)
                } else {
                    trySend(emptyList())
                }
            }
        awaitClose { registration.remove() }
    }.mapLatest { likedDtos ->
        if (likedDtos.isEmpty()) return@mapLatest emptyList<LikedItem>()

        val likedIds = likedDtos.mapNotNull { it.itemId }
        if (likedIds.isEmpty()) return@mapLatest emptyList<LikedItem>()

        val likedAtById = likedDtos.mapNotNull { dto -> dto.itemId?.let { it to dto.likedAt } }.toMap()
        val items = mutableListOf<LikedItem>()
        val chunks = likedIds.chunked(10)

        try {
            for (chunk in chunks) {
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
            items
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching liked items details", e)
            emptyList()
        }
    }
}

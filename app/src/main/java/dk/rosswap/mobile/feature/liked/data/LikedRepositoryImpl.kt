package dk.rosswap.mobile.feature.liked.data

import android.util.Log
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.liked.domain.LikedMapper
import dk.rosswap.mobile.feature.liked.domain.LikedRepository
import dk.rosswap.mobile.feature.liked.data.LikedDto
import dk.rosswap.mobile.feature.liked.domain.LikedItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LikedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LikedRepository {

    companion object {
        private const val TAG = "LikedRepositoryImpl"
    }

    override fun getLikedItems(userId: String): Flow<List<LikedItem>> = callbackFlow {
        Log.d(TAG, "Starting listener for user $userId")
        val registration = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Listen failed", e)
                    close(e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val rawLiked = snapshot.get("likedItemIds")
                    Log.d(TAG, "Got snapshot. likedItemIds type: ${rawLiked?.javaClass?.simpleName}, value: $rawLiked")
                    val likedDtos = (rawLiked as? List<*>)?.mapNotNull { LikedDto.fromAny(it) } ?: emptyList()
                    trySend(likedDtos)
                } else {
                    Log.d(TAG, "Snapshot null or doesn't exist")
                    trySend(emptyList())
                }
            }
        awaitClose { 
            Log.d(TAG, "Removing listener")
            registration.remove() 
        }
    }.map { likedDtos ->
        if (likedDtos.isEmpty()) {
            Log.d(TAG, "No liked DTOs found")
            return@map emptyList<LikedItem>()
        }

        // Filter out any empty IDs which cause Firestore query crashes
        val likedIds = likedDtos.mapNotNull { it.itemId }
            .filter { it.isNotBlank() }
            
        if (likedIds.isEmpty()) {
            Log.d(TAG, "No valid liked IDs found after filtering")
            return@map emptyList<LikedItem>()
        }
        
        Log.d(TAG, "Fetching details for ${likedIds.size} items: $likedIds")

        val likedAtById = likedDtos.mapNotNull { dto -> dto.itemId?.takeIf { it.isNotBlank() }?.let { it to dto.likedAt } }.toMap()
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
            Log.d(TAG, "Successfully fetched ${items.size} items")
            items
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e(TAG, "Error fetching liked items details", e)
            emptyList()
        }
    }
}

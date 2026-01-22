package dk.rosswap.mobile.feature.items.data

import android.util.Log
import dk.rosswap.mobile.core.common.SessionManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.core.mappers.ItemMapper as CoreItemMapper
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// Repo for item-related pages. Contains:
// - CRUD-like operations (get, add)
// - Behind-the-scenes operations irrelevant to the user per se
// - Stage Firestore communication
// Should not contain:
// - User-facing UseCases a la uploading images or creating items

class ItemsRepositoryImpl @Inject constructor(
    private val sessionManager: SessionManager,
    private val firestore: FirebaseFirestore
) : ItemsRepository {

    companion object {
        private const val TAG = "ItemsRepositoryImpl"
    }

    // a getter like this one will remain in items repository; it also just happens
    // to be the only function whose implementation is in the repo
    override suspend fun getLatestItems(limit: Long): Result<List<Item>> {
        suspend fun mapSnapshot(): Result<List<Item>> {
            // 1. get snapshot ("how things are right now") from firestore
            val snapshot = firestore
                .collection("items")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            // 2. map snapshot to items
            val items = snapshot.documents.mapNotNull { doc ->
                try {
                    CoreItemMapper.fromMap(doc.data ?: emptyMap(), doc.id)
                } catch (_: Exception) {
                    null
                }
            }

            // 3. log first 5 items for debugging
            items.take(5).forEach { item ->
                Log.d(TAG, "Fetched item id=${item.id} imageUrl='${item.imageUrl.take(120)}'")
            }

            // 4 return items!
            return Result.success(items)
        }

        return try {
            mapSnapshot()
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "Failed to load items (code=${e.code})", e)
            // 5. fallback:
            // same logic as before, except doing it a second time. in future we should
            // probably just do exponential backoff instead of repeating bloated code,
            // Doing it this way might be worth it cuz it can inadvertently cause offline
            // support; we get out-of-sync info from the remnant firestore object this way
            return try {
                val snapshot = firestore
                    .collection("items")
                    .limit(limit)
                    .get()
                    .await()

                val items = snapshot.documents.mapNotNull { doc ->
                    try {
                        CoreItemMapper.fromMap(doc.data ?: emptyMap(), doc.id)
                    } catch (_: Exception) {
                        null
                    }
                }

                Result.success(items)
            } catch (fallback: Exception) {
                Log.e(TAG, "Fallback load items failed", fallback)
                val combinedException = Exception(
                    "Failed to load items (original Firestore code=${e.code}): ${e.message}. " +
                        "Fallback also failed: ${fallback.message}",
                    e
                ).apply {
                    addSuppressed(fallback)
                }
                Result.failure(combinedException)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load items", e)
            Result.failure(e)
        }
    }
}

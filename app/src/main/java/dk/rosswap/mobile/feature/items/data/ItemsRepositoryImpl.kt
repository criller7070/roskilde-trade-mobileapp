package dk.rosswap.mobile.feature.items.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import dk.rosswap.mobile.feature.items.data.Item
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class ItemsRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ItemsRepository {

    companion object {
        private const val TAG = "ItemsRepositoryImpl"
    }

    private suspend fun resolveUserName(uid: String, authFallbackEmail: String?): String {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val fromUserDoc = (doc.getString("name") ?: doc.getString("userName"))
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            fromUserDoc
                ?: authFallbackEmail?.substringBefore('@')?.takeIf { it.isNotBlank() }
                ?: uid
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resolve userName from /users/$uid", e)
            authFallbackEmail?.substringBefore('@')?.takeIf { it.isNotBlank() } ?: uid
        }
    }

    override suspend fun createItem(
        title: String,
        description: String,
        imageUri: Uri?,
        mode: String
    ): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("You must be logged in to create an item."))

        val uid = user.uid
        val authName = user.displayName?.trim().orEmpty()
        val userName = if (authName.isNotBlank()) authName else resolveUserName(uid, user.email)

        if (imageUri == null) {
            return Result.failure(IllegalArgumentException("Image is required"))
        }

        return try {
            // Create item first to get a stable itemId for storage path
            val itemRef = firestore.collection("items").document()
            val itemId = itemRef.id

            val imageUrl = uploadImage(itemId, imageUri).getOrThrow()

            val item = hashMapOf(
                "title" to title,
                "description" to description,
                "imageUrl" to imageUrl,
                "mode" to mode,
                "userId" to uid,
                "userName" to userName,
                "createdAt" to FieldValue.serverTimestamp()
            )

            itemRef.set(item).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create item", e)
            Result.failure(e)
        }
    }

    private suspend fun uploadImage(itemId: String, uri: Uri): Result<String> {
        return try {
            val filename = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("items/$itemId/$filename")
            ref.putFile(uri).await()
            val downloadUri = ref.downloadUrl.await()
            Result.success(downloadUri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload item image", e)
            Result.failure(e)
        }
    }

    override suspend fun getLatestItems(limit: Long): Result<List<Item>> {
        suspend fun mapSnapshot(): Result<List<Item>> {
            val snapshot = firestore
                .collection("items")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { doc ->
                val title = doc.getString("title") ?: return@mapNotNull null
                val description = doc.getString("description") ?: ""
                val mode = doc.getString("mode") ?: "bytte"
                val imageUrl = doc.getString("imageUrl") ?: ""
                val userId = doc.getString("userId") ?: ""
                val userName = doc.getString("userName") ?: ""
                val createdAt = doc.getTimestamp("createdAt")

                Item(
                    id = doc.id,
                    title = title,
                    description = description,
                    mode = mode,
                    imageUrl = imageUrl,
                    userId = userId,
                    userName = userName,
                    createdAt = createdAt
                )
            }

            return Result.success(items)
        }

        return try {
            mapSnapshot()
        } catch (e: FirebaseFirestoreException) {
            // In some environments the deployed rules/indexes can differ from README.
            // Retrying without orderBy helps avoid missing-index issues while still letting the UI work.
            Log.e(TAG, "Failed to load items (code=${e.code})", e)

            return try {
                val snapshot = firestore
                    .collection("items")
                    .limit(limit)
                    .get()
                    .await()

                val items = snapshot.documents.mapNotNull { doc ->
                    val title = doc.getString("title") ?: return@mapNotNull null
                    val description = doc.getString("description") ?: ""
                    val mode = doc.getString("mode") ?: "bytte"
                    val imageUrl = doc.getString("imageUrl") ?: ""
                    val userId = doc.getString("userId") ?: ""
                    val userName = doc.getString("userName") ?: ""
                    val createdAt = doc.getTimestamp("createdAt")

                    Item(
                        id = doc.id,
                        title = title,
                        description = description,
                        mode = mode,
                        imageUrl = imageUrl,
                        userId = userId,
                        userName = userName,
                        createdAt = createdAt
                    )
                }

                // Surface original error code in case we need to fix rules/index later.
                Result.success(items)
            } catch (fallback: Exception) {
                Log.e(TAG, "Fallback load items failed", fallback)
                Result.failure(Exception("Failed to load items (${e.code}): ${e.message}", fallback))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load items", e)
            Result.failure(e)
        }
    }
}

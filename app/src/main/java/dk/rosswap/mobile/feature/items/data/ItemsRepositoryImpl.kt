package dk.rosswap.mobile.feature.items.data

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import dk.rosswap.mobile.feature.items.data.Item
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

class ItemsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ItemsRepository {

    companion object {
        private const val TAG = "ItemsRepositoryImpl"
        private const val MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB
        private val ALLOWED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
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

    private fun validateImageUri(uri: Uri): Result<Unit> {
        // Validate MIME type
        val mimeType = context.contentResolver.getType(uri)
        if (mimeType == null || !ALLOWED_IMAGE_TYPES.contains(mimeType.lowercase())) {
            // Fallback: check file extension if MIME type is not available
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            val mimeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mimeFromExtension == null || !ALLOWED_IMAGE_TYPES.contains(mimeFromExtension.lowercase())) {
                return Result.failure(
                    IllegalArgumentException(
                        "Only image files (JPEG, PNG, WebP) are allowed. Found: ${mimeType ?: "unknown"}"
                    )
                )
            }
        }

        // Validate file size
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                var fileSize = 0L
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    fileSize += bytesRead
                    // Early exit if file is too large
                    if (fileSize > MAX_IMAGE_SIZE_BYTES) {
                        val sizeMB = fileSize / (1024 * 1024)
                        return Result.failure(
                            IllegalArgumentException(
                                "Image file is too large (>${sizeMB}MB). Maximum allowed size is ${MAX_IMAGE_SIZE_BYTES / (1024 * 1024)}MB."
                            )
                        )
                    }
                }
            } ?: return Result.failure(IllegalArgumentException("Unable to read image file"))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check file size", e)
            return Result.failure(IllegalArgumentException("Unable to validate image file: ${e.message}"))
        }

        return Result.success(Unit)
    }

    private suspend fun uploadImage(itemId: String, uri: Uri): Result<String> {
        return try {
            // Validate image before uploading
            validateImageUri(uri).getOrElse { error ->
                return Result.failure(error)
            }

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val extension = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val filename = "${UUID.randomUUID()}.$extension"
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

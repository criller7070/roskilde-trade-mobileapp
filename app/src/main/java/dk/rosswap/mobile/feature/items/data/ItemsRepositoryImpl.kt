package dk.rosswap.mobile.feature.items.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import java.util.Locale
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.rosswap.mobile.feature.items.domain.ItemsRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ItemsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ItemsRepository {

    companion object {
        private const val TAG = "ItemsRepositoryImpl"
        private const val MIN_IMAGE_SIZE_BYTES = 1 * 1024 // 1 KB
        private const val MAX_IMAGE_SIZE_BYTES = 20 * 1024 * 1024 // 20 MB (matches web)
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

    private fun sanitizeFilename(original: String): String {
        // Keep it simple and compatible with the web validator: allow letters/digits, dash, underscore and dot.
        val trimmed = original.trim().take(120)
        val replaced = trimmed.replace(Regex("[^A-Za-z0-9._-]"), "-")
        // Avoid empty or dot-only names.
        val fallback = "image"
        val safe = replaced.trim('-').trim().takeIf { it.isNotBlank() && it.any { ch -> ch.isLetterOrDigit() } }
            ?: fallback
        return safe
    }

    private fun validateImageUri(uri: Uri): Result<Unit> {
        // Validate MIME type
        val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.US)
        if (mimeType == null || !ALLOWED_IMAGE_TYPES.contains(mimeType)) {
            // Fallback: check file extension if MIME type is not available
            val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            val mimeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.lowercase(Locale.US)
            if (mimeFromExtension == null || !ALLOWED_IMAGE_TYPES.contains(mimeFromExtension)) {
                val detectedType = mimeFromExtension ?: mimeType ?: "unknown"
                return Result.failure(
                    IllegalArgumentException(
                        "Only image files (JPEG, PNG, WebP) are allowed. Detected type: $detectedType"
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
                    if (fileSize in 1 until MIN_IMAGE_SIZE_BYTES) {
                        // keep reading until we know actual size
                    }
                    // Early exit if file is too large
                    if (fileSize > MAX_IMAGE_SIZE_BYTES) {
                        val sizeMB = String.format(Locale.US, "%.1f", fileSize / (1024.0 * 1024.0))
                        val maxSizeMB = String.format(Locale.US, "%.0f", MAX_IMAGE_SIZE_BYTES / (1024.0 * 1024.0))
                        return Result.failure(
                            IllegalArgumentException(
                                "Image file is too large (${sizeMB}MB). Maximum allowed size is ${maxSizeMB}MB."
                            )
                        )
                    }
                }

                if (fileSize < MIN_IMAGE_SIZE_BYTES) {
                    return Result.failure(
                        IllegalArgumentException("Image file is too small. Minimum allowed size is 1KB.")
                    )
                }
            } ?: return Result.failure(IllegalArgumentException("Unable to read image file"))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check file size", e)
            return Result.failure(IllegalArgumentException("Unable to validate image file: ${e.message}"))
        }

        return Result.success(Unit)
    }

    private fun getDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun uploadImage(uri: Uri): Result<String> {
        return try {
            // Validate image before uploading
            validateImageUri(uri).getOrElse { error ->
                return Result.failure(error)
            }

            val mimeType = context.contentResolver.getType(uri)?.lowercase(Locale.US) ?: "image/jpeg"
            val extension = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg" // Defaults to jpg for image/jpeg and any other image type
            }

            val originalName = getDisplayName(uri)
                ?: runCatching {
                    // Fallback: best-effort from URI string.
                    val urlExt = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                    if (urlExt.isNullOrBlank()) null else "image.$urlExt"
                }.getOrNull()

            val baseName = sanitizeFilename(originalName ?: "image.$extension")
            val timestamp = System.currentTimeMillis()
            val filename = "$timestamp-$baseName"

            // Web app convention: posts/{Date.now()}-{file.name}
            val ref = storage.reference.child("posts/$filename")
            ref.putFile(uri).await()
            val downloadUri = ref.downloadUrl.await()
            Log.d(TAG, "Uploaded image to posts/$filename, downloadUri=$downloadUri")
            Result.success(downloadUri.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload item image", e)
            Result.failure(e)
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
            val itemRef = firestore.collection("items").document()
            val imageUrl = uploadImage(imageUri).getOrThrow()
            Log.d(TAG, "Creating item ${itemRef.id} with imageUrl=$imageUrl")

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

            items.take(5).forEach { item ->
                Log.d(TAG, "Fetched item id=${item.id} imageUrl='${item.imageUrl.take(120)}'")
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

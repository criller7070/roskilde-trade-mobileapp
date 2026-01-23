package dk.rosswap.mobile.feature.items.domain

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import dk.rosswap.mobile.core.utils.FileNameUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UploadItemImageUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val storage: FirebaseStorage
) {
    // little util for getting the display name for the image
    private fun getDisplayName(uri: Uri): String? {
        return try {
            // just using context is quite lazy but it works for now
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
                }
        } catch (_: Exception) {
            null
        }
    }

    suspend operator fun invoke(uri: Uri): Result<String> {
        return try {
            // 1. streamline mime types
            val mimeType = context.contentResolver.getType(uri)?.lowercase() ?: "image/jpeg"
            val extension = when (mimeType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }

            // 2. get display name
            val originalName = getDisplayName(uri)
                ?: runCatching {
                    val urlExt = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
                    if (urlExt.isNullOrBlank()) null else "image.$urlExt"
                }.getOrNull()

            // 3. sanitize filename
            val baseName = FileNameUtil.sanitizeFilename(originalName ?: "image.$extension")
            val filename = "${System.currentTimeMillis()}-$baseName"

            // 4. send to storage!
            val ref = storage.reference.child("posts/$filename")
            ref.putFile(uri).await()
            val downloadUri = ref.downloadUrl.await()
            Result.success(downloadUri.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

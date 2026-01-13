package dk.rosswap.mobile.feature.chat.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import javax.inject.Inject
import java.io.ByteArrayOutputStream

class UploadChatImageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    companion object {
        private const val MAX_IMAGE_SIZE_BYTES = 10_000_000L // 10MB
        private const val COMPRESSION_QUALITY = 85
        private const val COMPRESSION_THRESHOLD_BYTES = 2_000_000L // 2MB
    }

    suspend operator fun invoke(
        chatId: String,
        senderId: String,
        fileName: String,
        bytes: ByteArray
    ): Result<String> {
        // Validate file size
        if (bytes.size > MAX_IMAGE_SIZE_BYTES) {
            return Result.failure(
                IllegalArgumentException("Image must be smaller than 10MB. Current: ${(bytes.size / 1_000_000.0).toInt()}MB")
            )
        }

        // Validate file name/extension
        val extension = fileName.substringAfterLast(".", "").lowercase()
        val allowedExtensions = listOf("jpg", "jpeg", "png", "webp")
        if (extension.isEmpty() || !allowedExtensions.contains(extension)) {
            return Result.failure(
                IllegalArgumentException("Image must be in JPEG, PNG, or WebP format.")
            )
        }

        // Compress if needed
        val finalBytes = if (bytes.size > COMPRESSION_THRESHOLD_BYTES) {
            compressImage(bytes)
        } else {
            bytes
        }

        return try {
            // Upload image to Firebase Storage
            val imageUrl = repository.uploadChatImage(
                chatId = chatId,
                fileName = fileName,
                bytes = finalBytes
            )

            // Create message document with image
            val messageId = repository.sendImageMessage(
                chatId = chatId,
                senderId = senderId,
                imageUrl = imageUrl
            )

            Result.success(messageId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressImage(bytes: ByteArray): ByteArray {
        var bitmap: Bitmap? = null
        return try {
            // Decode the image bytes into a Bitmap
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return bytes // If decoding fails, return original
            
            // Compress the bitmap with specified quality
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
            
            outputStream.toByteArray()
        } catch (e: Exception) {
            // If compression fails for any reason, return original bytes
            bytes
        } finally {
            // Clean up the bitmap to free memory
            bitmap?.recycle()
        }
    }
}


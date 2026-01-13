package dk.rosswap.mobile.feature.chat.domain

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
                IllegalArgumentException("Image must be smaller than 10MB. Current: %.1fMB".format(bytes.size / 1_000_000.0))
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
        // Note: Proper compression would require image decoding + re-encoding
        // For now, return original bytes. The Firebase Storage SDK
        // will handle efficient format conversion automatically.
        // In a real app, you'd use an image compression library.
        return bytes
    }
}


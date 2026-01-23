package dk.rosswap.mobile.feature.chat.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.tasks.await

class UploadChatImageUseCase @Inject constructor(
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore
) {
    companion object {
        // again should probably not be hardcoded and moved to a const util/holder
        private const val MAX_IMAGE_SIZE_BYTES = 10_000_000L // 10MB
        private const val COMPRESSION_QUALITY = 85 // %
        private const val COMPRESSION_THRESHOLD_BYTES = 2_000_000L // 2MB
    }

    suspend operator fun invoke(
        chatId: String,
        senderId: String,
        fileName: String,
        bytes: ByteArray
    ): Result<String> {
        // A. VALIDATION.
        // the truth is that we validate just to match the web app. In retrospect there isn't
        // really much of a point; Firebas probably already validates size, extensions and
        // compresses. But because we're not sure, we might as well do Validation.

        // B. Validate file size
        if (bytes.size > MAX_IMAGE_SIZE_BYTES) {
            return Result.failure(
                IllegalArgumentException("Image must be smaller than 10MB. Current: %.1fMB".format(bytes.size / 1_000_000.0))
            )
        }

        // C. Validate file name/extension with our dedicated util
        val extension = fileName.substringAfterLast(".", "").lowercase()
        val allowedExtensions = listOf("jpg", "jpeg", "png", "webp")
        if (extension.isEmpty() || !allowedExtensions.contains(extension)) {
            return Result.failure(
                IllegalArgumentException("Image must be in JPEG, PNG, or WebP format.")
            )
        }

        // D. Compress if needed
        val finalBytes = if (bytes.size > COMPRESSION_THRESHOLD_BYTES) {
            compressImage(bytes)
        } else {
            bytes
        }

        return try {
            // 1. First, upload image to Firebase Storage
            val uploadRef = storage.reference.child("chatPhotos/$chatId/$fileName")
            uploadRef.putBytes(finalBytes).await()
            val imageUrl = uploadRef.downloadUrl.await().toString()

            // 2. Create chat document with image and update chat metadata (batch write)
            val chatDocRef = firestore.collection("chats").document(chatId)
            // use server timestamp instead of client Timestamp.now()
            val batch = firestore.batch()

            // the exact batch write logic matches SendMessageUseCase
            val messageRef = chatDocRef.collection("messages").document()
            val messageData = mapOf(
                "senderId" to senderId,
                "text" to null,
                "imageUrl" to imageUrl,
                "timestamp" to FieldValue.serverTimestamp()
            )
            batch.set(messageRef, messageData)

            // include sender in participants to satisfy typical security rules
            batch.set(
                chatDocRef,
                mapOf(
                    "participants" to FieldValue.arrayUnion(senderId),
                    "lastMessage" to "[Image]",
                    "lastMessageTime" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )

            // 3. Get participants to update both users' chat lists
            val chatSnap = chatDocRef.get().await() // snapshot, i.e. "how it looks right now"
            val participants = chatSnap.get("participants") as? List<*>
            val otherUserId = participants
                ?.mapNotNull { it as? String }
                ?.firstOrNull { it != senderId }

            val senderUserChatRef = firestore
                .collection("userChats")
                .document(senderId)
                .collection("chats")
                .document(chatId)

            batch.set(
                senderUserChatRef,
                mapOf(
                    "lastMessage" to "[Image]",
                    "lastMessageTime" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )


            batch.commit().await()
            Result.success(messageRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun compressImage(bytes: ByteArray): ByteArray {
        // if you're not in the know, images consist of an array of Bytes; RGB and black/white
        // these arrays correspond in position to a 2D grid - a map of bits, if you will.
        var bitmap: Bitmap? = null
        return try {
            // 1. decode the image
            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return bytes // if decoding fails, return original

            // 2. compress the image
            // images are sent to and from a ByteStream, in our case just for the output
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)

            // 3. get the compressed bytes
            val compressedBytes = outputStream.toByteArray()
            if (compressedBytes.size < bytes.size) compressedBytes else bytes // fallback
        } catch (_: Exception) {
            bytes
        } finally {
            bitmap?.recycle()
        }
    }
}

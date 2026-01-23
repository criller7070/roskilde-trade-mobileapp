package dk.rosswap.mobile.feature.chat.domain

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class SendMessageUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend operator fun invoke(chatId: String, senderId: String, text: String): Result<Unit> {
        // Sending a message will be a long and arduous journey. Get ready:
        // 1. start trimming. If empty after trimming, nothing to send
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.success(Unit)

        Log.d(TAG, "sendMessage: chatId=$chatId sender=$senderId textLen=${'$'}{trimmed.length}")

        // 2. Retry logic with exponential backoff like in the web app. What this means is
        // you keep retrying, but after each time it gets slower. This actually fixes bugs
        // because if the network is slow, it'll try again later
        return retryWithBackoff<Unit>(
            maxAttempts = 3, // these hardcoded values should probably be centralized in
            initialDelayMs = 1000L // a const util or in core/common
        ) {
            try {
                // 3. check and get chat document
                val chatDocRef = firestore.collection("chats").document(chatId)

                val existing = chatDocRef.get().await()
                val participants = (existing.get("participants") as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()

                // 4. Fallback: if participants are missing, create minimal chat meta so rules/reads succeed
                if (participants.isEmpty()) {
                    chatDocRef.set(
                        mapOf(
                            "participants" to listOf(senderId),
                            "lastMessage" to trimmed,
                            "lastMessageTime" to FieldValue.serverTimestamp()
                        ),
                        com.google.firebase.firestore.SetOptions.merge()
                    ).await()
                }

                // 5. We batch together message + chat metadata (atomic write)
                val batch = firestore.batch()
                val messageRef = chatDocRef.collection("messages").document()
                val messageData = mapOf(
                    "senderId" to senderId,
                    "text" to trimmed,
                    "content" to trimmed,
                    "imageUrl" to null,
                    "timestamp" to FieldValue.serverTimestamp() // use server ts
                )

                // 5. message write!
                batch.set(messageRef, messageData)

                // 6. update main chat doc (web uses metadata like lastMessage/lastMessageTime)
                batch.set(
                    chatDocRef,
                    mapOf(
                        "participants" to FieldValue.arrayUnion(senderId),
                        "lastMessage" to trimmed,
                        "lastMessageTime" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                // 7. set other user's chat doc (if any)
                val otherUserId = participants.firstOrNull { it != senderId }

                // 8. get sender's chat doc
                val senderUserChatRef = firestore
                    .collection("userChats")
                    .document(senderId)
                    .collection("chats")
                    .document(chatId)

                // 9. update sender's chat doc
                batch.set(
                    senderUserChatRef,
                    mapOf(
                        "lastMessage" to trimmed,
                        "lastMessageTime" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )

                Log.d(TAG, "sendMessage: committing batch for chatId=$chatId")

                // 10 everything went well, execute batch!
                batch.commit().await()
            } catch (e: FirebaseFirestoreException) {
                // Surface Firestore error codes in logs and map rate-limit errors for UI handling
                val wrappedMessage = "Firestore error [${'$'}{e.code}]: ${'$'}{e.message}"
                Log.w(TAG, wrappedMessage, e)

                // if it's rate limiting, convert to IllegalStateException for ViewModel handling
                if (e.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED) {
                    throw IllegalStateException("Rate limit exceeded: ${e.message}", e)
                }

                throw e
            }
        }
    }

    private suspend inline fun <T> retryWithBackoff(
        maxAttempts: Int,
        initialDelayMs: Long,
        block: suspend () -> T
    ): Result<T> {
        var lastException: Exception? = null

        for (attempt in 1..maxAttempts) {
            try {
                return Result.success(block())
            } catch (e: Exception) {
                // Log the failure for easier diagnosis during runtime
                Log.w(TAG, "send attempt ${'$'}attempt failed: ${'$'}{e.message}", e)

                // If Firestore returns a non-retryable error (permission, invalid arg,
                // failed precondition), bail out immediately and surface the error.
                if (e is FirebaseFirestoreException) {
                    when (e.code) {
                        FirebaseFirestoreException.Code.PERMISSION_DENIED,
                        FirebaseFirestoreException.Code.INVALID_ARGUMENT,
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION -> {
                            return Result.failure(e)
                        }
                        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> {
                            // Treat rate-limit as non-transient for UX reasons
                            return Result.failure(IllegalStateException("Rate limit: ${'$'}{e.message}", e))
                        }
                        else -> {
                            // treat as potentially transient and allow retry
                        }
                    }
                }

                lastException = e

                if (attempt < maxAttempts) {
                    val delayMs = initialDelayMs * (1L shl (attempt - 1))
                    val jitterMs = (Math.random() * 500).toLong()
                    val totalDelayMs = delayMs + jitterMs
                    delay(totalDelayMs)
                }
            }
        }

        // if something went horribly wrong:
        return Result.failure(lastException ?: Exception("Message send failed after $maxAttempts attempts"))
    }

    companion object {
        private const val TAG = "SendMessageUseCase"
    }
}

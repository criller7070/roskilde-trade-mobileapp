package dk.rosswap.mobile.feature.chat.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
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

        // 2. Retry logic with exponential backoff like in the web app. What this means is
        // you keep retrying, but after each time it gets slower. This actually fixes bugs
        // because if the network is slow, it'll try again later
        return retryWithBackoff<Unit>(
            maxAttempts = 3, // these hardcoded values should probably be centralized in
            initialDelayMs = 1000L // a const util or in core/common
        ) {
            // 3. check and get chat document
            val chatDocRef = firestore.collection("chats").document(chatId)

            val existing = chatDocRef.get().await()
            val participants = (existing.get("participants") as? List<*>)
                ?.mapNotNull { it as? String }
                .orEmpty()

            // 3.5 Fallback: fix empty fields
            if (participants.isEmpty()) {
                chatDocRef.set(
                    mapOf(
                        "participants" to listOf(senderId),
                        "lastMessage" to trimmed,
                        "lastMessageTime" to Timestamp.now()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                ).await()
            }
            val nowTs = Timestamp.now()

            // 4. We batch together message + chat metadata like in the web app
            // the idea of batch commiting (i.e. either doing all or nothing) is
            // twofold: For one, it's way faster. But it also avoids half-finished
            // docs or one uploading one user's metadata, kind of like SQL's transactions
            val batch = firestore.batch()
            val messageRef = chatDocRef.collection("messages").document()
            val messageData = mapOf(
                "senderId" to senderId,
                "text" to trimmed,
                "content" to trimmed,
                "imageUrl" to null,
                "timestamp" to nowTs // Ts = Timestamp
            )

            // 5. message write!
            batch.set(messageRef, messageData)

            // 6. update main chat doc (web uses metadata like lastMessage/lastMessageTime)
            batch.set(
                chatDocRef,
                mapOf(
                    "lastMessage" to trimmed,
                    "lastMessageTime" to nowTs
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
                    "lastMessageTime" to nowTs
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )

            // 9.5 Fallback behavior for other user's chat doc (if any)
            if (otherUserId != null) {
                val otherUserChatRef = firestore
                    .collection("userChats")
                    .document(otherUserId)
                    .collection("chats")
                    .document(chatId)

                batch.set(
                    otherUserChatRef,
                    mapOf(
                        "lastMessage" to trimmed,
                        "lastMessageTime" to nowTs,
                        "unreadCount" to FieldValue.increment(1)
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }

            // 10 everything went well, execute batch!
            batch.commit().await()
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
                lastException = e

                if (attempt < maxAttempts) {
                    // backoff retry is really  just a simple "algorithmic" job.
                    // every app ever uses the Jitter technique where we
                    // pick a random number inside the max wait time M, which
                    // is doubled per attempt. Also used in the web app.
                    // 1L shl (attempt - 1) is just a fancy way of 2^attempt,
                    // it's called a "left-shift operator")
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
}

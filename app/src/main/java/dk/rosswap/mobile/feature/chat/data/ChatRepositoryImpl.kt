package dk.rosswap.mobile.feature.chat.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import dk.rosswap.mobile.core.utils.GenerateChatIdUtil
import dk.rosswap.mobile.feature.chat.domain.ChatMessage
import dk.rosswap.mobile.feature.chat.domain.ChatMessageMapper
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import dk.rosswap.mobile.feature.chat.domain.UserChat
import dk.rosswap.mobile.feature.chat.domain.UserChatMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ChatRepository {

    override fun observeChatList(userId: String): Flow<List<UserChat>> = callbackFlow {
        val query = firestore
            .collection("userChats")
            .document(userId)
            .collection("chats")
            .orderBy("lastMessageTime", com.google.firebase.firestore.Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot
                ?.documents
                ?.map(UserChatMapper::fromUserChatDoc)
                .orEmpty()

            trySend(items)
        }

        awaitClose { registration.remove() }
    }

    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        val query = firestore
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot
                ?.documents
                ?.map(ChatMessageMapper::fromMessageDoc)
                .orEmpty()

            // Send initial items immediately (may contain storage paths)
            trySend(items)

            // Asynchronously resolve any Firebase storage paths (gs:// or storage-relative)
            CoroutineScope(Dispatchers.IO).launch {
                val resolved = items.toMutableList()
                var changed = false
                for (i in resolved.indices) {
                    val msg = resolved[i]
                    val img = msg.imageUrl
                    if (!img.isNullOrBlank() && !img.startsWith("http://") && !img.startsWith("https://")) {
                        try {
                            val ref = if (img.startsWith("gs://")) storage.getReferenceFromUrl(img) else storage.reference.child(img)
                            val downloadUri = ref.downloadUrl.await()
                            resolved[i] = msg.copy(imageUrl = downloadUri.toString())
                            changed = true
                        } catch (e: Exception) {
                            // If resolution fails, ignore and keep original value (adapter will handle fallback)
                        }
                    }
                }

                if (changed) {
                    trySend(resolved)
                }
            }
        }

        awaitClose { registration.remove() }
    }

    override fun generateChatId(userAId: String, userBId: String, itemId: String): String {
        return GenerateChatIdUtil.generate(userAId, userBId, itemId)
            .getOrElse {
                val (minId, maxId) = if (userAId <= userBId) userAId to userBId else userBId to userAId
                "${minId}_${maxId}_${itemId}"
            }
    }

    override suspend fun openChat(
        currentUserId: String,
        otherUserId: String,
        itemId: String,
        itemName: String?,
        itemImage: String?,
        currentUserName: String?,
        otherUserName: String?
    ): Result<String> {
        return runCatching {
            val chatId = GenerateChatIdUtil.generate(currentUserId, otherUserId, itemId).getOrThrow()
            ensureChatExists(
                chatId = chatId,
                currentUserId = currentUserId,
                otherUserId = otherUserId,
                itemId = itemId,
                itemName = itemName,
                itemImage = itemImage,
                currentUserName = currentUserName,
                otherUserName = otherUserName
            )
            chatId
        }
    }

    override suspend fun ensureChatExists(
        chatId: String,
        currentUserId: String,
        otherUserId: String,
        itemId: String,
        itemName: String?,
        itemImage: String?,
        currentUserName: String?,
        otherUserName: String?
    ) {
        if (chatId.isBlank()) return

        val chatDocRef = firestore.collection("chats").document(chatId)

        val participants = listOf(currentUserId, otherUserId).distinct()

        val userNames = buildMap<String, String> {
            if (!currentUserName.isNullOrBlank()) put(currentUserId, currentUserName)
            if (!otherUserName.isNullOrBlank()) put(otherUserId, otherUserName)
        }

        val data = mutableMapOf<String, Any>(
            "participants" to participants,
            "itemId" to itemId
        )

        if (!itemName.isNullOrBlank()) data["itemName"] = itemName
        if (!itemImage.isNullOrBlank()) data["itemImage"] = itemImage
        if (userNames.isNotEmpty()) data["userNames"] = userNames

        data["createdAt"] = FieldValue.serverTimestamp()

        chatDocRef.set(data, SetOptions.merge()).await()

        val now = Timestamp.now()

        val currentUserRef = firestore
            .collection("userChats")
            .document(currentUserId)
            .collection("chats")
            .document(chatId)

        val otherUserRef = firestore
            .collection("userChats")
            .document(otherUserId)
            .collection("chats")
            .document(chatId)

        val currentUserChatData = mutableMapOf<String, Any>(
            "chatId" to chatId,
            "itemId" to itemId,
            "lastMessageTime" to now
        )
        if (!itemName.isNullOrBlank()) currentUserChatData["itemName"] = itemName
        if (!itemImage.isNullOrBlank()) currentUserChatData["itemImage"] = itemImage
        if (!otherUserName.isNullOrBlank()) currentUserChatData["otherUserName"] = otherUserName
        // don't set unreadCount here (leave as-is)
        currentUserRef.set(currentUserChatData, SetOptions.merge()).await()

        val otherUserChatData = mutableMapOf<String, Any>(
            "chatId" to chatId,
            "itemId" to itemId,
            "lastMessageTime" to now
        )
        if (!itemName.isNullOrBlank()) otherUserChatData["itemName"] = itemName
        if (!itemImage.isNullOrBlank()) otherUserChatData["itemImage"] = itemImage
        if (!currentUserName.isNullOrBlank()) otherUserChatData["otherUserName"] = currentUserName
        otherUserRef.set(otherUserChatData, SetOptions.merge()).await()
    }

    override suspend fun sendTextMessage(chatId: String, senderId: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        val chatDocRef = firestore.collection("chats").document(chatId)

        val existing = chatDocRef.get().await()
        val participants = (existing.get("participants") as? List<*>)
            ?.mapNotNull { it as? String }
            .orEmpty()

        if (participants.isEmpty()) {
            chatDocRef.set(
                mapOf(
                    "participants" to listOf(senderId),
                    "lastMessage" to trimmed,
                    "lastMessageTime" to Timestamp.now()
                ),
                SetOptions.merge()
            ).await()
        }

        val now = Timestamp.now()

        // We use a batch to update message + chat metadata together.
        val batch = firestore.batch()

        val messageRef = chatDocRef.collection("messages").document()

        val messageData = mapOf(
            "senderId" to senderId,
            "text" to trimmed,
            "content" to trimmed,
            "imageUrl" to null,
            "timestamp" to now
        )

        // message write
        batch.set(messageRef, messageData)

        // update main chat doc (web uses metadata like lastMessage/lastMessageTime)
        batch.set(
            chatDocRef,
            mapOf(
                "lastMessage" to trimmed,
                "lastMessageTime" to now
            ),
            SetOptions.merge()
        )

        val otherUserId = participants.firstOrNull { it != senderId }

        val senderUserChatRef = firestore
            .collection("userChats")
            .document(senderId)
            .collection("chats")
            .document(chatId)

        batch.set(
            senderUserChatRef,
            mapOf(
                "lastMessage" to trimmed,
                "lastMessageTime" to now
            ),
            SetOptions.merge()
        )

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
                    "lastMessageTime" to now,
                    "unreadCount" to FieldValue.increment(1)
                ),
                SetOptions.merge()
            )
        }

        batch.commit().await()
    }

    override suspend fun deleteChatFromUserList(userId: String, chatId: String) {
        firestore
            .collection("userChats")
            .document(userId)
            .collection("chats")
            .document(chatId)
            .delete()
            .await()
    }

    override suspend fun markChatRead(userId: String, chatId: String) {
        val uid = userId.trim()
        val cid = chatId.trim()
        if (uid.isBlank() || cid.isBlank()) return

        firestore
            .collection("userChats")
            .document(uid)
            .collection("chats")
            .document(cid)
            .set(
                mapOf(
                    "unreadCount" to 0L
                ),
                SetOptions.merge()
            )
            .await()
    }

    override suspend fun uploadChatImage(chatId: String, fileName: String, bytes: ByteArray): String {
        val uploadRef = storage.reference.child("chatPhotos/$chatId/$fileName")
        uploadRef.putBytes(bytes).await()
        return uploadRef.downloadUrl.await().toString()
    }

    override suspend fun sendImageMessage(chatId: String, senderId: String, imageUrl: String): String {
        val chatDocRef = firestore.collection("chats").document(chatId)
        val now = Timestamp.now()

        // Create message with image
        val batch = firestore.batch()

        val messageRef = chatDocRef.collection("messages").document()
        val messageData = mapOf(
            "senderId" to senderId,
            "text" to null,
            "imageUrl" to imageUrl,
            "timestamp" to now
        )
        batch.set(messageRef, messageData)

        // Update chat metadata
        batch.set(
            chatDocRef,
            mapOf(
                "lastMessage" to "[Image]",
                "lastMessageTime" to now
            ),
            SetOptions.merge()
        )

        // Get participants to update both users' chat lists
        val chatSnap = chatDocRef.get().await()
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
                "lastMessageTime" to now
            ),
            SetOptions.merge()
        )

        if (otherUserId != null) {
            val otherUserChatRef = firestore
                .collection("userChats")
                .document(otherUserId)
                .collection("chats")
                .document(chatId)

            batch.set(
                otherUserChatRef,
                mapOf(
                    "lastMessage" to "[Image]",
                    "lastMessageTime" to now,
                    "unreadCount" to FieldValue.increment(1)
                ),
                SetOptions.merge()
            )
        }

        batch.commit().await()
        return messageRef.id
    }
}

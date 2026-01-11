package dk.rosswap.mobile.feature.chat.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dk.rosswap.mobile.feature.chat.domain.ChatMessage
import dk.rosswap.mobile.feature.chat.domain.ChatMessageMapper
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import dk.rosswap.mobile.feature.chat.domain.UserChat
import dk.rosswap.mobile.feature.chat.domain.UserChatMapper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
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

            trySend(items)
        }

        awaitClose { registration.remove() }
    }

    override fun generateChatId(userAId: String, userBId: String, itemId: String): String {
        val (minId, maxId) = if (userAId <= userBId) userAId to userBId else userBId to userAId
        return "${minId}_${maxId}_${itemId}" // must match web
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

        val chatSnap = chatDocRef.get().await()
        val updatedParticipants = chatSnap.get("participants") as? List<*>
        val otherUserId = updatedParticipants
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
}

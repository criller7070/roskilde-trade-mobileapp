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

// A note on how Repositories are designed per our "culture choices":
// - Easy-to-understand and user-direct functions have their UseCase
// - Behind-the-scenes functions go into repository to not clog up directory
// - CRUD-like operations should stay in the repository to not clog up directory
// - If a behind-the-scenes function is too big, it can be a UseCase

// NB: chat functionality in general has lots of behind-the-scenes files

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ChatRepository {

    // Here we mimic the observer pattern: Instead of constantly checking for
    // changes, each file simple observes and waits for changes
    override fun observeChatList(userId: String): Flow<List<UserChat>> = callbackFlow {
        // first, get values immediately from firestore
        val query = firestore
            .collection("userChats")
            .document(userId)
            .collection("chats")
            .orderBy("lastMessageTime", com.google.firebase.firestore.Query.Direction.DESCENDING)

        // then, register/subscribe to observation, which means the class
        // listens for Firebase DB "snapshots", i.e. "how things are right now"
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener // in-built Firebase keyword
            }

            // get the items for our snapshot
            val items = snapshot
                ?.documents
                ?.map(UserChatMapper::fromUserChatDoc)
                .orEmpty()

            // notify observers
            trySend(items)
        }

        awaitClose { registration.remove() }
    }

    // function fulfills more or less the same function but for individual
    // messages when you open a chat
    override fun observeMessages(chatId: String): Flow<List<ChatMessage>> = callbackFlow {
        // same as before: get values immediately from firestore
        val query = firestore
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)

        // then, register/subscribe to observation,
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            // get the items for our snapshot
            val items = snapshot
                ?.documents
                ?.map(ChatMessageMapper::fromMessageDoc)
                .orEmpty()

            // send initial items immediately (may contain storage paths)
            trySend(items)

            // patch: resolve any Firebase storage paths (gs:// or storage-relative)
            // its not really a must but I think it solved a bug once
            CoroutineScope(Dispatchers.IO).launch { // get scope
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
                        } catch (_: Exception) {
                            // eh whatever
                        }
                    }
                }

                if (changed) {
                    trySend(resolved)
                }
            }
        }

        awaitClose { registration.remove() } // unsubscribe
    }

    // kind of a wrapper for the util in common/utils but it's good to register it in repo
    override fun generateChatId(userAId: String, userBId: String, itemId: String): String {
        return GenerateChatIdUtil.generate(userAId, userBId, itemId)
            .getOrElse {
                val (minId, maxId) = if (userAId <= userBId) userAId to userBId else userBId to userAId
                "${minId}_${maxId}_${itemId}"
            }
    }

    // openChat is in reality a very fancy getter. It gets or generates ChatId (because we
    // manually create it) for userA and userB per itemId
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

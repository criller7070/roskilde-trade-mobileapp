package dk.rosswap.mobile.feature.chat.data

import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.chat.domain.Chat
import dk.rosswap.mobile.feature.chat.domain.ChatMapper
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    override fun observeChatList(userId: String): Flow<List<Chat>> = callbackFlow {
        val query = firestore
            .collection("userChats")
            .document(userId)
            .collection("chats")
            // Prefer most recent chats first if the field exists.
            .orderBy("lastMessageTime", com.google.firebase.firestore.Query.Direction.DESCENDING)

        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot
                ?.documents
                ?.map(ChatMapper::fromUserChatDoc)
                .orEmpty()

            trySend(items)
        }

        awaitClose { registration.remove() }
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

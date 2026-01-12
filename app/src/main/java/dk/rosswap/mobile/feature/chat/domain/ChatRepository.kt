package dk.rosswap.mobile.feature.chat.domain

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChatList(userId: String): Flow<List<UserChat>>

    fun observeMessages(chatId: String): Flow<List<ChatMessage>>

    fun generateChatId(userAId: String, userBId: String, itemId: String): String

    suspend fun openChat(
        currentUserId: String,
        otherUserId: String,
        itemId: String,
        itemName: String? = null,
        itemImage: String? = null,
        currentUserName: String? = null,
        otherUserName: String? = null
    ): Result<String>

    suspend fun ensureChatExists(
        chatId: String,
        currentUserId: String,
        otherUserId: String,
        itemId: String,
        itemName: String? = null,
        itemImage: String? = null,
        currentUserName: String? = null,
        otherUserName: String? = null
    )

    suspend fun markChatRead(userId: String, chatId: String)

    suspend fun sendTextMessage(chatId: String, senderId: String, text: String)

    suspend fun deleteChatFromUserList(userId: String, chatId: String)
}

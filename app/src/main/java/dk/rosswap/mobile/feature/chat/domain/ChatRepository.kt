package dk.rosswap.mobile.feature.chat.domain

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChatList(userId: String): Flow<List<UserChat>>

    fun observeMessages(chatId: String): Flow<List<ChatMessage>>

    suspend fun deleteChatFromUserList(userId: String, chatId: String)
}

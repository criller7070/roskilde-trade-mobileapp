package dk.rosswap.mobile.feature.chat.domain

import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeChatList(userId: String): Flow<List<Chat>>

    suspend fun deleteChatFromUserList(userId: String, chatId: String)
}

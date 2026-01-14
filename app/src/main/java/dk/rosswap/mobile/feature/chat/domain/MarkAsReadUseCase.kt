package dk.rosswap.mobile.feature.chat.domain

import javax.inject.Inject

class MarkAsReadUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(userId: String, chatId: String) = repository.markChatRead(userId = userId, chatId = chatId)
}


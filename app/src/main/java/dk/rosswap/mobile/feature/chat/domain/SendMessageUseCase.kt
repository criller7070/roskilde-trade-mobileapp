package dk.rosswap.mobile.feature.chat.domain

import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, senderId: String, text: String) =
        repository.sendTextMessage(chatId = chatId, senderId = senderId, text = text)
}


package dk.rosswap.mobile.feature.chat.domain

import javax.inject.Inject

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    private val lastSentTimestamps = mutableMapOf<String, Long>()
    private val rateLimitMillis = 1500L

    suspend operator fun invoke(chatId: String, senderId: String, text: String) {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 500) { "Message must be between 1 and 500 characters." }

        val now = System.currentTimeMillis()
        val lastSent = lastSentTimestamps[senderId]
        if (lastSent != null && now - lastSent < rateLimitMillis) {
            throw IllegalStateException("You're sending messages too quickly—give it a moment.")
        }

        repository.sendTextMessage(chatId = chatId, senderId = senderId, text = trimmed)
        lastSentTimestamps[senderId] = now
    }
}

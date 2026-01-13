package dk.rosswap.mobile.feature.chat.domain

import javax.inject.Inject

class ObserveMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(chatId: String) = repository.observeMessages(chatId)
}


package dk.rosswap.mobile.feature.chat.domain

import javax.inject.Inject

class ObserveChatsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(userId: String) = repository.observeChatList(userId)
}


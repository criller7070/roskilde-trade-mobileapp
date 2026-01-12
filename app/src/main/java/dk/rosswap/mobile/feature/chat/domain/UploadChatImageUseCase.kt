package dk.rosswap.mobile.feature.chat.domain

import javax.inject.Inject

class UploadChatImageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(chatId: String, fileName: String, bytes: ByteArray): String {
        return repository.uploadChatImage(chatId = chatId, fileName = fileName, bytes = bytes)
    }
}


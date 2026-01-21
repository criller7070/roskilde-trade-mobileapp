package dk.rosswap.mobile.feature.chat.domain

import com.google.firebase.firestore.DocumentSnapshot
import dk.rosswap.mobile.feature.chat.data.ChatMessageDto

object ChatMessageMapper {
    fun fromMessageDoc(doc: DocumentSnapshot): ChatMessage {
        val dto = ChatMessageDto.fromDoc(doc)
        return ChatMessage(
            id = doc.id,
            senderId = dto.senderId,
            text = dto.text,
            imageUrl = dto.imageUrl,
            timestamp = dto.timestamp
        )
    }
}

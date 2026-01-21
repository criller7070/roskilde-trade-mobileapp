package dk.rosswap.mobile.feature.chat.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class ChatMessageDto(
    val senderId: String? = null,
    val text: String? = null,
    val imageUrl: String? = null,
    val timestamp: Timestamp? = null
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): ChatMessageDto {
            val textValue = doc.getString("text") ?: doc.getString("content")
            return ChatMessageDto(
                senderId = doc.getString("senderId"),
                text = textValue,
                imageUrl = doc.getString("imageUrl"),
                timestamp = doc.get("timestamp") as? Timestamp
            )
        }
    }
}
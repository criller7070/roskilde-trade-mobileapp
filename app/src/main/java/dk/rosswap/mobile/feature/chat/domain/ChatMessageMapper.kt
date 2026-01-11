package dk.rosswap.mobile.feature.chat.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

object ChatMessageMapper {

    fun fromMessageDoc(doc: DocumentSnapshot): ChatMessage {
        return ChatMessage(
            id = doc.id,
            senderId = doc.getString("senderId"),
            text = doc.getString("text"),
            imageUrl = doc.getString("imageUrl"),
            timestamp = doc.get("timestamp").toTimestampFlexibleOrNull()
        )
    }
}

private fun Any?.toTimestampFlexibleOrNull(): Timestamp? {
    return when (this) {
        is Timestamp -> this
        is Number -> Timestamp(this.toLong(), 0)
        else -> null
    }
}


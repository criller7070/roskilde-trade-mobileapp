package dk.rosswap.mobile.feature.chat.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

object ChatMapper {

    fun fromUserChatDoc(doc: DocumentSnapshot): Chat {
        return Chat(
            id = doc.id,
            itemId = doc.getString("itemId"),
            itemName = doc.getString("itemName"),
            itemImage = doc.getString("itemImage"),
            otherUserName = doc.getString("otherUserName"),
            lastMessage = doc.getString("lastMessage"),
            lastMessageTime = doc.get("lastMessageTime").toTimestampFlexibleOrNull(),
            unreadCount = doc.getLong("unreadCount") ?: 0L
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

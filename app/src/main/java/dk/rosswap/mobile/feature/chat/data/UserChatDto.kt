package dk.rosswap.mobile.feature.chat.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

// NB: Represents entire conversations between users. The reason for this
// strange naming convention is because we want to match the nomenclature
// from our web app.

data class UserChatDto(
    val itemId: String? = null,
    val itemName: String? = null,
    val itemImage: String? = null,
    val otherUserName: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Timestamp? = null,
    val unreadCount: Long = 0L
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): UserChatDto {
            return UserChatDto(
                itemId = doc.getString("itemId"),
                itemName = doc.getString("itemName"),
                itemImage = doc.getString("itemImage"),
                otherUserName = doc.getString("otherUserName"),
                lastMessage = doc.getString("lastMessage"),
                lastMessageTime = doc.get("lastMessageTime") as? Timestamp,
                unreadCount = doc.getLong("unreadCount") ?: 0L
            )
        }
    }
}
package dk.rosswap.mobile.feature.chat.domain

import com.google.firebase.firestore.DocumentSnapshot
import dk.rosswap.mobile.feature.chat.data.UserChatDto

object UserChatMapper {

    fun fromUserChatDoc(doc: DocumentSnapshot): UserChat {
        val dto = UserChatDto.fromDoc(doc)
        return UserChat(
            id = doc.id,
            itemId = dto.itemId,
            itemName = dto.itemName,
            itemImage = dto.itemImage,
            otherUserName = dto.otherUserName,
            lastMessage = dto.lastMessage,
            lastMessageTime = dto.lastMessageTime,
            unreadCount = dto.unreadCount
        )
    }
}

package dk.rosswap.mobile.feature.chat.domain

import com.google.firebase.Timestamp

data class Chat(
    val id: String = "",
    val itemId: String? = null,
    val itemName: String? = null,
    val itemImage: String? = null,
    val otherUserName: String? = null,
    val lastMessage: String? = null,
    val lastMessageTime: Timestamp? = null,
    val unreadCount: Long = 0L
)

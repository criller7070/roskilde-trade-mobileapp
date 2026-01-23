package dk.rosswap.mobile.feature.chat.domain

import com.google.firebase.Timestamp

// domain model for individual chats inside a conversation

data class ChatMessage(
    val id: String = "",
    val senderId: String? = null,
    val text: String? = null,
    val imageUrl: String? = null,
    val timestamp: Timestamp? = null
)

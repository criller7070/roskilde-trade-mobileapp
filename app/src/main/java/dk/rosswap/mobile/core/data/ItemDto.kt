package dk.rosswap.mobile.core.data

import com.google.firebase.Timestamp

data class ItemDto(
    val title: String? = null,
    val description: String? = null,
    val mode: String? = null,
    val imageUrl: String? = null,
    val userId: String? = null,
    val userName: String? = null,
    val createdAt: Timestamp? = null,
    val price: Double = 0.0
)

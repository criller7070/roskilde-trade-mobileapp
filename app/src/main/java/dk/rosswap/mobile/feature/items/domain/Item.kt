package dk.rosswap.mobile.feature.items.domain

import com.google.firebase.Timestamp

data class Item(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val mode: String = "", // "bytte", "salg", etc.
    val imageUrl: String = "",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Timestamp? = null
)

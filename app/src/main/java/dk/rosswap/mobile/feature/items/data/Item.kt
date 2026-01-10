package dk.rosswap.mobile.feature.items.data

import com.google.firebase.Timestamp

data class Item(
    val id: String,
    val title: String,
    val description: String,
    val mode: String,
    val imageUrl: String,
    val userId: String,
    val userName: String,
    val createdAt: Timestamp?
)
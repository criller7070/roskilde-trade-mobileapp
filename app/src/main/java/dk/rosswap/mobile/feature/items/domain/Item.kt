package dk.rosswap.mobile.feature.items.domain

import com.google.firebase.Timestamp

/**
 * Shared Item model for both feed and liked posts.
 * Includes default values for Firestore 'toObjects' compatibility.
 */
data class Item(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val mode: String = "", // "bytte", "sælge", etc.
    val imageUrl: String = "",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Timestamp? = null,
    // Add price if needed for sales, default 0.0 for now
    val price: Double = 0.0
)

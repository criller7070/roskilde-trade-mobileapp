package dk.rosswap.mobile.core.common

import com.google.firebase.Timestamp

/**
 * Data class representing a user in the app.
 * Mirrors the Firestore structure at /users/{userId}
 */
data class User(
    val uid: String,
    val name: String,
    val email: String,
    val photoURL: String,
    val createdAt: Timestamp? = null,
    val gdprConsent: Boolean = false,
    val consentedAt: Timestamp? = null,
    val likedItemIds: List<String> = emptyList(),
    val dislikedItemIds: List<String> = emptyList(),
    val isAnonymous: Boolean = false
)

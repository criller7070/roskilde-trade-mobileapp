package dk.rosswap.mobile.feature.liked.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlin.collections.get

data class LikedDto(
    val itemId: String? = null,
    val likedAt: Timestamp? = null
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): LikedDto {
            return LikedDto(
                itemId = doc.getString("itemId") ?: doc.id,
                likedAt = doc.get("likedAt") as? Timestamp
            )
        }

        fun fromAny(value: Any?): LikedDto? {
            return when (value) {
                is String -> LikedDto(itemId = value)
                is Map<*, *> -> LikedDto(
                    itemId = value["itemId"] as? String,
                    likedAt = value["likedAt"] as? Timestamp
                )
                else -> null
            }
        }
    }
}
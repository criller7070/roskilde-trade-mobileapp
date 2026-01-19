package dk.rosswap.mobile.feature.liked.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class DislikedDto(
    val itemId: String? = null,
    val dislikedAt: Timestamp? = null
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): DislikedDto {
            return DislikedDto(
                itemId = doc.getString("itemId") ?: doc.id,
                dislikedAt = doc.get("dislikedAt") as? Timestamp
            )
        }

        fun fromAny(value: Any?): DislikedDto? {
            return when (value) {
                is String -> DislikedDto(itemId = value)
                is Map<*, *> -> DislikedDto(
                    itemId = value["itemId"] as? String,
                    dislikedAt = value["dislikedAt"] as? Timestamp
                )
                else -> null
            }
        }
    }
}


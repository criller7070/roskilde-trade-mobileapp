package dk.rosswap.mobile.feature.bugreport.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

data class BugReportDto(
    val description: String? = null,
    val imageUrl: String? = null,
    val userId: String? = null,
    val createdAt: Timestamp? = null
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): BugReportDto {
            val data = doc.data ?: emptyMap<String, Any?>()
            return BugReportDto(
                description = data["description"] as? String,
                imageUrl = data["imageUrl"] as? String,
                userId = data["userId"] as? String,
                createdAt = data["createdAt"] as? Timestamp
            )
        }
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "description" to description,
            "imageUrl" to imageUrl,
            "userId" to userId,
            "createdAt" to createdAt
        )
    }
}
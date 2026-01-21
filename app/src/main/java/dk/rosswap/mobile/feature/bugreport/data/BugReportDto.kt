package dk.rosswap.mobile.feature.bugreport.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

// BugReportDto <-> [BugReport mapping] <-> BugReport.kt

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

    // like User.kt, we currently have no use for toMap() and just
    // keep it there for consistency
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "description" to description,
            "imageUrl" to imageUrl,
            "userId" to userId,
            "createdAt" to createdAt
        )
    }
}
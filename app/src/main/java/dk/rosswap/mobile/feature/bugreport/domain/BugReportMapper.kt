package dk.rosswap.mobile.feature.bugreport.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import dk.rosswap.mobile.feature.bugreport.data.BugReportDto

object BugReportMapper {

    fun fromDoc(doc: DocumentSnapshot): BugReport {
        val dto = BugReportDto.fromDoc(doc)
        return BugReport(
            id = doc.id,
            description = dto.description ?: "",
            imageUrl = dto.imageUrl,
            userId = dto.userId ?: "",
            userName = (doc.getString("userName") ?: doc.getString("userName")),
            userEmail = doc.getString("userEmail"),
            status = doc.getString("status") ?: "",
            createdAt = dto.createdAt
        )
    }

    fun toMap(report: BugReport): Map<String, Any?> {
        val m = mutableMapOf<String, Any?>()
        m["description"] = report.description
        m["imageUrl"] = report.imageUrl
        m["userId"] = report.userId
        m["userName"] = report.userName
        m["userEmail"] = report.userEmail
        m["status"] = report.status
        m["createdAt"] = report.createdAt ?: Timestamp.now()
        return m
    }
}

package dk.rosswap.mobile.feature.bugreport.domain

import com.google.firebase.Timestamp

data class BugReport(
    val id: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val userId: String = "",
    val userName: String? = null,
    val userEmail: String? = null,
    val status: String = "",
    val createdAt: Timestamp? = null
)


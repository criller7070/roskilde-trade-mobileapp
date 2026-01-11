package dk.rosswap.mobile.feature.items.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    var title: String = "",
    var description: String = "",
    var imageUrl: String = "",
    var mode: String = "",
    var userId: String = "",
    var userName: String = "",
    var createdAt: Timestamp? = null
)

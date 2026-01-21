package dk.rosswap.mobile.feature.account.domain

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import dk.rosswap.mobile.core.model.User
import dk.rosswap.mobile.feature.account.data.AccountDto

// Simple mapper between DTOs and domain User objects.

object AccountMapper {

    fun fromDoc(doc: DocumentSnapshot): User {
        val dto = AccountDto.fromDoc(doc)
        return User(
            uid = dto.uid,
            name = dto.name,
            email = dto.email,
            photoURL = "",
            createdAt = dto.createdAt,
            gdprConsent = dto.gdprConsent,
            consentedAt = dto.consentedAt,
            likedItemIds = emptyList(),
            dislikedItemIds = emptyList(),
            emailVerified = dto.emailVerified,
            isAnonymous = false
        )
    }

    fun toMap(user: User): Map<String, Any?> {
        val m = mutableMapOf<String, Any?>()
        m["uid"] = user.uid
        m["name"] = user.name
        m["email"] = user.email.lowercase()
        if (user.photoURL.isNotEmpty()) m["photoURL"] = user.photoURL
        m["createdAt"] = user.createdAt ?: Timestamp.now()
        m["gdprConsent"] = user.gdprConsent
        user.consentedAt?.let { m["consentedAt"] = it }
        if (user.likedItemIds.isNotEmpty()) m["likedItemIds"] = user.likedItemIds
        if (user.dislikedItemIds.isNotEmpty()) m["dislikedItemIds"] = user.dislikedItemIds
        m["isAnonymous"] = user.isAnonymous
        m["emailVerified"] = user.emailVerified
        return m
    }
}

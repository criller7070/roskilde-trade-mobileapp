package dk.rosswap.mobile.feature.account.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

// DTOs come from and go to Firestore. This is just a container for the data,
// which is mapped to a User object in /core. We could have included a UserAccount
// domain model, but it did not make much sense.

data class AccountDto(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val emailVerified: Boolean = false,
    val createdAt: Timestamp? = null,
    val consentedAt: Timestamp? = null,
    val gdprConsent: Boolean = false,
    val photoURL: String = ""
) {
    companion object {
        fun fromDoc(doc: DocumentSnapshot): AccountDto {
            val data = doc.data ?: emptyMap<String, Any?>()
            return AccountDto(
                // we're being lenient here with the types; we accept both
                // name and userName, photoURL and photoUrl etc just as a quick fix
                uid = (data["uid"] as? String) ?: doc.id,
                name = (data["name"] as? String) ?: (data["userName"] as? String) ?: "",
                email = (data["email"] as? String) ?: "",
                emailVerified = data["emailVerified"] as? Boolean ?: false,
                createdAt = data["createdAt"] as? Timestamp,
                consentedAt = data["consentedAt"] as? Timestamp,
                gdprConsent = data["gdprConsent"] as? Boolean ?: false,
                photoURL = (data["photoURL"] as? String) ?: (data["photoUrl"] as? String) ?: ""
            )
        }
    }
}
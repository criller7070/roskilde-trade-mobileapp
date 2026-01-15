package dk.rosswap.mobile.core.common

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val photoURL: String = "",
    val createdAt: Timestamp? = null,
    val gdprConsent: Boolean = false,
    val consentedAt: Timestamp? = null,
    val likedItemIds: List<String> = emptyList(),
    val dislikedItemIds: List<String> = emptyList(),
    val emailVerified: Boolean = false,
    val isAnonymous: Boolean = false
) : Parcelable

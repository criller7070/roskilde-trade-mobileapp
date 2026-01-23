package dk.rosswap.mobile.core.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize // Recommended by Android Studio.
data class Item(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val mode: String = "",
    val imageUrl: String = "",
    val userId: String = "",
    val userName: String = "",
    val createdAt: Timestamp? = null,
    val price: Double = 0.0
) : Parcelable
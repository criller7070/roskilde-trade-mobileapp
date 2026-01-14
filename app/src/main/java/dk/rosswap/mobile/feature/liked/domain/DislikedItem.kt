package dk.rosswap.mobile.feature.liked.domain

import com.google.firebase.Timestamp
import dk.rosswap.mobile.core.model.Item

data class DislikedItem(
    val item: Item,
    val dislikedAt: Timestamp?
)
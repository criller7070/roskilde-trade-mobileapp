package dk.rosswap.mobile.feature.liked.domain

import com.google.firebase.Timestamp
import dk.rosswap.mobile.core.model.Item

// One thing about this feature; there's lots of files, and they're small or boilerplate.
// Next time we consider bunching up LikedItem or DislikedItem into a kind of "Reaction",
// with Reaction.liked or something like that somewhat like Result<> (success or failure)

data class DislikedItem(
    val item: Item,
    val dislikedAt: Timestamp?
)
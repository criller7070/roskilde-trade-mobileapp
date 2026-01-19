package dk.rosswap.mobile.feature.liked.domain

import dk.rosswap.mobile.core.model.Item

interface LikedRepository {
    suspend fun getLikedItems(userId: String): Result<List<LikedItem>>
}

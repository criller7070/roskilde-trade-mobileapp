package dk.rosswap.mobile.feature.liked.domain

import dk.rosswap.mobile.core.common.Item

interface LikedRepository {
    suspend fun getLikedItems(userId: String): Result<List<Item>>
}

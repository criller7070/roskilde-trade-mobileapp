package dk.rosswap.mobile.feature.liked.domain

import dk.rosswap.mobile.feature.items.domain.Item

interface LikedRepository {
    suspend fun getLikedItems(userId: String): Result<List<Item>>
    suspend fun getDislikedItems(userId: String): Result<List<Item>>
}

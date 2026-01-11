package dk.rosswap.mobile.feature.liked.domain

import dk.rosswap.mobile.feature.items.domain.Item

interface LikedRepository {
    suspend fun getLikedItems(userId: String): Result<List<Item>>
    suspend fun likeItem(userId: String, itemId: String): Result<Unit>
    suspend fun unlikeItem(userId: String, itemId: String): Result<Unit>
}

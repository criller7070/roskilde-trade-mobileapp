package dk.rosswap.mobile.feature.liked.domain

import dk.rosswap.mobile.core.model.Item
import kotlinx.coroutines.flow.Flow

interface LikedRepository {
    fun getLikedItems(userId: String): Flow<List<LikedItem>>
}

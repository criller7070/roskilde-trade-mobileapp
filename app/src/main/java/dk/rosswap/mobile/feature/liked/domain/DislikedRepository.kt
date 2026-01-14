package dk.rosswap.mobile.feature.liked.domain

import dk.rosswap.mobile.core.common.Item

interface DislikedRepository {
    suspend fun getDislikedItems(userId: String): Result<List<Item>>
}

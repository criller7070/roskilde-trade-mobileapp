package dk.rosswap.mobile.feature.items.domain

import dk.rosswap.mobile.core.model.Item

interface ItemsRepository {
    suspend fun getLatestItems(limit: Long = 50): Result<List<Item>>
}

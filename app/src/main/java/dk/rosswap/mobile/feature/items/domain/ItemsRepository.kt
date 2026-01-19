package dk.rosswap.mobile.feature.items.domain

import dk.rosswap.mobile.core.model.Item

interface ItemsRepository {
    suspend fun createItem(
        title: String,
        description: String,
        imageUri: android.net.Uri?,
        mode: String,
    ): Result<Unit>

    suspend fun getLatestItems(limit: Long = 50): Result<List<Item>>
}

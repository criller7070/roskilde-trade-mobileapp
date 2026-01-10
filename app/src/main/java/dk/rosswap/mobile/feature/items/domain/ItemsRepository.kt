package dk.rosswap.mobile.feature.items.domain

import android.net.Uri
import dk.rosswap.mobile.feature.items.data.Item

interface ItemsRepository {
    suspend fun createItem(
        title: String,
        description: String,
        imageUri: Uri?,
        mode: String,
    ): Result<Unit>

    suspend fun getLatestItems(limit: Long = 50): Result<List<Item>>
}

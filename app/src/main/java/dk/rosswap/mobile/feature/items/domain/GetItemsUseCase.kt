package dk.rosswap.mobile.feature.items.domain

import javax.inject.Inject

class GetItemsUseCase @Inject constructor(
    private val repository: ItemsRepository
) {
    suspend operator fun invoke(limit: Long = 50) = repository.getLatestItems(limit)
}


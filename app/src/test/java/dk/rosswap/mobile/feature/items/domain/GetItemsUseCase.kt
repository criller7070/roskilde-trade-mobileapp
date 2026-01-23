package dk.rosswap.mobile.feature.items.domain

import dk.rosswap.mobile.core.model.Item

// Test-only stub so unit tests can mock/override the behavior without pulling in DI.
class GetItemsUseCase {
    operator fun invoke(limit: Long = 50): Result<List<Item>> = Result.success(emptyList())
}

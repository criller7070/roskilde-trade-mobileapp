package dk.rosswap.mobile.feature.liked.domain

interface DislikedRepository {
    suspend fun getDislikedItems(userId: String): Result<List<DislikedItem>>
}

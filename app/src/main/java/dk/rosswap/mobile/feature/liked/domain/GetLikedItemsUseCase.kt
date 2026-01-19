package dk.rosswap.mobile.feature.liked.domain

import dk.rosswap.mobile.core.common.SessionManager
import javax.inject.Inject

class GetLikedItemsUseCase @Inject constructor(
    private val repository: LikedRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(userId: String): Result<List<LikedItem>> = repository.getLikedItems(userId)

    suspend operator fun invoke(): Result<List<LikedItem>> {
        val uid = sessionManager.currentUserId() ?: return Result.success(emptyList())
        return invoke(uid)
    }
}

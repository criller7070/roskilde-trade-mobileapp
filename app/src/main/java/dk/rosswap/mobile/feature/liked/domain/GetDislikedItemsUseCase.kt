package dk.rosswap.mobile.feature.liked.domain

import dk.rosswap.mobile.core.common.SessionManager
import javax.inject.Inject

class GetDislikedItemsUseCase @Inject constructor(
    private val dislikedRepository: DislikedRepository,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(): Result<List<DislikedItem>> {
        val uid = sessionManager.currentUserId() ?: return Result.failure(IllegalStateException("Not logged in"))
        return dislikedRepository.getDislikedItems(uid)
    }
}

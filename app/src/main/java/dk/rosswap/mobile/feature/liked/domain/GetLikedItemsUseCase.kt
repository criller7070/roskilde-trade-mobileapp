package dk.rosswap.mobile.feature.liked.domain

import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import javax.inject.Inject

class GetLikedItemsUseCase @Inject constructor(
    private val repository: LikedRepository,
    private val sessionManager: SessionManager
) {
    operator fun invoke(): Flow<List<LikedItem>> {
        val userId = sessionManager.currentUserId() ?: return emptyFlow()
        return repository.getLikedItems(userId)
    }
}

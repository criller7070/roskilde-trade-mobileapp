package dk.rosswap.mobile.feature.liked.domain

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class LikeItemUseCase @Inject constructor(
    private val repository: LikedRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(itemId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not logged in"))
        return repository.likeItem(userId, itemId)
    }
}

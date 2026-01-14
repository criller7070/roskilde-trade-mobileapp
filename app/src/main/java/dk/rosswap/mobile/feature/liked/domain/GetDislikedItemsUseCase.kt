package dk.rosswap.mobile.feature.liked.domain

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class GetDislikedItemsUseCase @Inject constructor(
    private val dislikedRepository: DislikedRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(): Result<List<DislikedItem>> {
        val userId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not logged in"))
        return dislikedRepository.getDislikedItems(userId)
    }
}

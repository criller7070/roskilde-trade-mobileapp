package dk.rosswap.mobile.feature.liked.domain

import com.google.firebase.auth.FirebaseAuth
import dk.rosswap.mobile.feature.items.domain.Item
import javax.inject.Inject

class GetLikedItemsUseCase @Inject constructor(
    private val repository: LikedRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(): Result<List<Item>> {
        val userId = auth.currentUser?.uid ?: return Result.success(emptyList())
        return repository.getLikedItems(userId)
    }
}

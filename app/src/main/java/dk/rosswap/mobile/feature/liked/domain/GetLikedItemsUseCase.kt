package dk.rosswap.mobile.feature.liked.domain

import com.google.firebase.auth.FirebaseAuth
import dk.rosswap.mobile.core.model.Item
import javax.inject.Inject

class GetLikedItemsUseCase @Inject constructor(
    private val repository: LikedRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(userId: String): Result<List<Item>> = repository.getLikedItems(userId)

    suspend operator fun invoke(): Result<List<Item>> {
        val uid = auth.currentUser?.uid ?: return Result.success(emptyList())
        return invoke(uid)
    }
}

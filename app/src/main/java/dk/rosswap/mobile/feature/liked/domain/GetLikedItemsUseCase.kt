package dk.rosswap.mobile.feature.liked.domain

import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject

class GetLikedItemsUseCase @Inject constructor(
    private val repository: LikedRepository,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(userId: String): Result<List<LikedItem>> = repository.getLikedItems(userId)

    suspend operator fun invoke(): Result<List<LikedItem>> {
        val uid = auth.currentUser?.uid ?: return Result.success(emptyList())
        return invoke(uid)
    }
}

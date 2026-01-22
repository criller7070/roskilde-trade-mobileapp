package dk.rosswap.mobile.feature.liked.domain

import android.util.Log
import javax.inject.Inject

class SwipeUseCase @Inject constructor(
    private val likeItemUseCase: LikeItemUseCase,
    private val unlikeItemUseCase: UnlikeItemUseCase,
    private val dislikeItemUseCase: DislikeItemUseCase,
    private val unDislikeItemUseCase: UndislikeItemUseCase
) {
    suspend fun swipeRight(itemId: String): Result<Unit> {
        // Prefer ensuring the liked state is set first, then remove any dislike.
        return try {
            val likeResult = likeItemUseCase(itemId)
            if (likeResult.isFailure) return likeResult

            // best-effort remove dislike; don't treat removal failure as fatal
            val removeDislikeResult = unDislikeItemUseCase(itemId)
            if (removeDislikeResult.isFailure) {
                Log.w(TAG, "swipeRight: failed to remove dislike for $itemId: ${removeDislikeResult.exceptionOrNull()?.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "swipeRight error", e)
            Result.failure(e)
        }
    }

    suspend fun swipeLeft(itemId: String): Result<Unit> {
        // Prefer ensuring the disliked state is set first, then remove any like.
        return try {
            val dislikeResult = dislikeItemUseCase(itemId)
            if (dislikeResult.isFailure) return dislikeResult

            // best-effort remove like
            val removeLikeResult = unlikeItemUseCase(itemId)
            if (removeLikeResult.isFailure) {
                Log.w(TAG, "swipeLeft: failed to remove like for $itemId: ${removeLikeResult.exceptionOrNull()?.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "swipeLeft error", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SwipeUseCase"
    }
}

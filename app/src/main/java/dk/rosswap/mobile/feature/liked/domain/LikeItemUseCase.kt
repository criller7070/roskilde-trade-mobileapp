package dk.rosswap.mobile.feature.liked.domain

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LikeItemUseCase @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(itemId: String): Result<Unit> {
        val userId = sessionManager.currentUserId() ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            firestore.collection("users").document(userId)
                .update("likedItemIds", FieldValue.arrayUnion(itemId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error liking item", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "LikeItemUseCase"
    }
}

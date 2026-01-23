package dk.rosswap.mobile.feature.liked.domain

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UnlikeItemUseCase @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) {
    suspend operator fun invoke(itemId: String): Result<Unit> {
        // check if user is logged in and get user id if so
        val userId = sessionManager.currentUserId() ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            // do the actual removal here
            val data = mapOf("likedItemIds" to FieldValue.arrayRemove(itemId))
            firestore.collection("users").document(userId)
                .set(data, SetOptions.merge())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unliking item", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "UnlikeItemUseCase"
    }
}

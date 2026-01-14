package dk.rosswap.mobile.feature.liked.domain

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class UnDislikeItemUseCase @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend operator fun invoke(itemId: String): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not logged in"))
        return try {
            firestore.collection("users").document(userId)
                .update("dislikedItemIds", FieldValue.arrayRemove(itemId))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing dislike", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "RemoveDislikeItemUseCase"
    }
}

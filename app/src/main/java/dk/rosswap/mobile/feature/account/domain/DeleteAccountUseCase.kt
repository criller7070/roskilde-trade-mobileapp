package dk.rosswap.mobile.feature.account.domain

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val sessionManager: SessionManager
) {
    companion object {
        private const val TAG = "DeleteAccountUseCase"
        private const val USERS_COLLECTION = "users"
        private const val ITEMS_COLLECTION = "items"
    }
    suspend operator fun invoke(targetUserId: String): Result<Unit> {
        val current = sessionManager.currentUserId() ?: return Result.failure(IllegalStateException("Not signed in"))
        if (current != targetUserId) return Result.failure(SecurityException("Not authorized"))

        return try {
            // 1) delete user's items (and associated images)
            // We iterate user's items and attempt to delete any image referenced by the item.
            // Image deletion errors are logged and the document deletion proceeds.
            val itemsSnap = firestore.collection(ITEMS_COLLECTION)
                .whereEqualTo("userId", targetUserId)
                .get()
                .await()

            for (doc in itemsSnap.documents) {
                try {
                    val data = doc.data ?: emptyMap<String, Any?>()
                    val imageUrl = (data["imageUrl"] as? String) ?: ""
                    if (imageUrl.isNotBlank()) {
                        try {
                            val imgRef = storage.getReferenceFromUrl(imageUrl)
                            // delete image from storage; do not fail the entire flow if it fails
                            imgRef.delete().await()
                            Log.d(TAG, "Deleted image for item=${doc.id}")
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed deleting image for item=${doc.id}: ${e.message}")
                        }
                    }

                    // delete Firestore document for this item
                    doc.reference.delete().await()
                    Log.d(TAG, "Deleted item doc id=${doc.id}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed deleting item doc ${doc.id}: ${e.message}")
                }
            }

            // 2) delete user Firestore document
            val userDocRef = firestore.collection(USERS_COLLECTION).document(targetUserId)
            val userSnap = userDocRef.get().await()
            if (userSnap.exists()) {
                userDocRef.delete().await()
                Log.d(TAG, "Deleted user doc id=$targetUserId")
            }

            // 3) delete Firebase auth user using SessionManager (which calls authRepo.deleteCurrentUser)
            val res = sessionManager.deleteAccount()
            if (res.isFailure) return Result.failure(res.exceptionOrNull()!!)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed delete account: ${e.message}", e)
            Result.failure(e)
        }
    }
}

package dk.rosswap.mobile.feature.items.domain

import android.content.Context
import android.net.Uri
import dk.rosswap.mobile.core.common.SessionManager
import dk.rosswap.mobile.core.mappers.ItemMapper as CoreItemMapper
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.model.Item
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AddItemUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
    private val uploadItemImageUseCase: UploadItemImageUseCase
) {
    // little util for getting username associated with the image
    private suspend fun resolveUserName(uid: String, authFallbackEmail: String?): String {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val fromUserDoc = (doc.getString("name") ?: doc.getString("userName"))
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            fromUserDoc
                ?: authFallbackEmail?.substringBefore('@')?.takeIf { it.isNotBlank() }
                ?: uid
        } catch (e: Exception) {
            authFallbackEmail?.substringBefore('@')?.takeIf { it.isNotBlank() } ?: uid
        }
    }

    suspend operator fun invoke(
        title: String,
        description: String,
        imageUri: Uri?,
        type: String
    ): Result<Unit> {
        // 1. check if user is logged in
        val uid = sessionManager.currentUserId()
            ?: return Result.failure(IllegalStateException("You must be logged in to create an item."))

        // 2. get user info
        val authName = (sessionManager.authState.value as? AuthState.Authenticated)?.user?.name?.trim().orEmpty()
        val authEmail = (sessionManager.authState.value as? AuthState.Authenticated)?.user?.email
        val userName = authName.ifBlank { resolveUserName(uid, authEmail) }

        // 3. check if all required fields are filled out
        if (imageUri == null) {
            return Result.failure(IllegalArgumentException("Image is required"))
        }

        return try {
            // 4. get image info
            val itemRef = firestore.collection("items").document()
            val imageUrl = uploadItemImageUseCase(imageUri).getOrThrow()

            // 5. get item info
            val domainItem = Item(
                id = itemRef.id,
                title = title.trim(),
                description = description.trim(),
                mode = type.lowercase(),
                imageUrl = imageUrl,
                userId = uid,
                userName = userName,
                createdAt = null
            )
            val coreDto = CoreItemMapper.toDto(domainItem)

            // 6. map item info to firebase
            val itemMap = mutableMapOf<String, Any?>()
            itemMap["title"] = coreDto.title
            itemMap["description"] = coreDto.description
            itemMap["mode"] = coreDto.mode
            itemMap["imageUrl"] = coreDto.imageUrl
            itemMap["userId"] = coreDto.userId
            itemMap["userName"] = coreDto.userName
            itemMap["createdAt"] = FieldValue.serverTimestamp()

            // 7. send!
            itemRef.set(itemMap).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

package dk.rosswap.mobile.feature.chat.domain

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class MarkAsReadUseCase @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    // this use case is in some way a fancy CRUD operation, and could theoretically stay
    // in the repository, but it's too user-facing to put there.
    suspend operator fun invoke(userId: String, chatId: String) {
        val uid = userId.trim()
        val cid = chatId.trim()
        if (uid.isBlank() || cid.isBlank()) return

        firestore
            .collection("userChats")
            .document(uid)
            .collection("chats")
            .document(cid)
            .set(
                mapOf(
                    "unreadCount" to 0L
                ),
                SetOptions.merge()
            )
            .await()
    }
}

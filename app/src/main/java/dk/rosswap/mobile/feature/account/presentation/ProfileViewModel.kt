package dk.rosswap.mobile.feature.account.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File

class ProfileViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // ---------------- USER ----------------

    val user
        get() = auth.currentUser

    // ---------------- PROFILE IMAGE ----------------

    private val _photoUrl = MutableLiveData<String?>()
    val photoUrl: LiveData<String?> = _photoUrl

    init {
        _photoUrl.value = user?.photoUrl?.toString()
    }

    fun uploadProfilePicture(uri: Uri) {
        val currentUser = user ?: return
        val uid = currentUser.uid

        val ref = storage.reference.child("profilePictures/$uid")

        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val profileUpdates = userProfileChangeRequest {
                    photoUri = downloadUri
                }

                currentUser.updateProfile(profileUpdates)
                    .addOnSuccessListener {
                        _photoUrl.value = downloadUri.toString()
                    }
                    .addOnFailureListener { exception ->
                        android.util.Log.e(
                            "ProfileViewModel",
                            "Failed to update profile photo",
                            exception
                        )
                    }
            }
    }

    // ---------------- GDPR EXPORT ----------------

    fun exportUserData(
        context: Context,
        onSuccess: (File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val currentUser = user ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val uid = currentUser.uid
                val result = JSONObject()

                // ---- AUTH PROFILE (Firebase Auth) ----
                result.put(
                    "authProfile",
                    JSONObject(
                        mapOf(
                            "uid" to uid,
                            "email" to currentUser.email,
                            "displayName" to currentUser.displayName,
                            "photoUrl" to currentUser.photoUrl?.toString(),
                            "providers" to currentUser.providerData.map { it.providerId }
                        )
                    )
                )

                // ---- USER DOCUMENT (Firestore) ----
                val userDoc = firestore.collection("users")
                    .document(uid)
                    .get()
                    .await()

                result.put("user", userDoc.data)

                // ---- ITEMS (POSTS) ----
                val itemsSnapshot = firestore.collection("items")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()

                result.put("items", itemsSnapshot.documents.map { it.data })

                // ---- CHATS ----
                val chatsSnapshot = firestore.collection("chats")
                    .whereArrayContains("participants", uid)
                    .get()
                    .await()

                result.put("chats", chatsSnapshot.documents.map { it.data })

                // ---- USER ↔ CHAT MAP ----
                val userChatsSnapshot = firestore.collection("userChats")
                    .document(uid)
                    .get()
                    .await()

                result.put("userChats", userChatsSnapshot.data)

                // ---- BUG REPORTS ----
                val bugReportsSnapshot = firestore.collection("bugReports")
                    .whereEqualTo("userId", uid)
                    .get()
                    .await()

                result.put("bugReports", bugReportsSnapshot.documents.map { it.data })

                // ---- WRITE FILE ----
                val file = File(
                    context.cacheDir,
                    "rosswap_user_data_$uid.json"
                )

                file.writeText(result.toString(2))

                viewModelScope.launch(Dispatchers.Main) {
                    onSuccess(file)
                }

            } catch (e: Exception) {
                e.printStackTrace()

                viewModelScope.launch(Dispatchers.Main) {
                    onError(e)
                }
            }
        }
    }



    // ---------------- DELETE ACCOUNT ----------------

    fun deleteAccount(
        onSuccess: () -> Unit,
        onReauthRequired: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val currentUser = auth.currentUser ?: return

        currentUser.delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                if (exception is FirebaseAuthRecentLoginRequiredException) {
                    onReauthRequired()
                } else {
                    onError(exception)
                }
            }
    }
}

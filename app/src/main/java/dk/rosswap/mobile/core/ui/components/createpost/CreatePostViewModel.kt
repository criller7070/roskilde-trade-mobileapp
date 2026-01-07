package dk.rosswap.mobile.core.ui.components.createpost

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _postCreated = MutableLiveData<Boolean>()
    val postCreated: LiveData<Boolean> = _postCreated

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun createPost(title: String, description: String, imageUri: Uri?, type: String) {
        _isLoading.value = true

        if (imageUri != null) {
            uploadImage(imageUri) { imageUrl ->
                savePostToFirestore(title, description, imageUrl, type)
            }
        } else {
            // Create post without image
            savePostToFirestore(title, description, null, type)
        }
    }

    private fun uploadImage(uri: Uri, onComplete: (String) -> Unit) {
        val filename = UUID.randomUUID().toString()
        val ref = storage.reference.child("post_images/$filename")

        ref.putFile(uri)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    onComplete(downloadUri.toString())
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
                _postCreated.value = false
            }
    }

    private fun savePostToFirestore(title: String, description: String, imageUrl: String?, type: String) {
        val post = hashMapOf(
            "title" to title,
            "description" to description,
            "imageUrl" to (imageUrl ?: ""),
            "type" to type,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        firestore.collection("posts")
            .add(post)
            .addOnSuccessListener {
                _isLoading.value = false
                _postCreated.value = true
            }
            .addOnFailureListener {
                _isLoading.value = false
                _postCreated.value = false
            }
    }
}

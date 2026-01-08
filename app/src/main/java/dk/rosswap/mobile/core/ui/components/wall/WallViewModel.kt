// Kotlin
package dk.rosswap.mobile.core.ui.components.wall

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.*

data class Post(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val author: String,
    val createdAt: Date = Date()
)

class WallViewModel : ViewModel() {

    private val _posts = MutableLiveData<List<Post>>().apply {
        value = listOf(
            Post(
                id = "1",
                title = "Øl",
                description = "Jeg vil gerne bytte denne øl med en anden øl. Gerne en guld dame.",
                type = "Bytte",
                author = "Hannah Lund"
            ),
            Post(
                id = "2",
                title = "Plastic ring",
                description = "M green, lavet af recycled plastic!!!",
                type = "Bytte",
                author = "Criller Hyllested"
            )
        )
    }
    val posts: LiveData<List<Post>> = _posts

    fun setPosts(list: List<Post>) {
        _posts.value = list
    }
}

package dk.rosswap.mobile.core.ui.components.liked

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LikedViewModel @Inject constructor() : ViewModel() {

    // Simple representation of liked posts. Replace with real model/repo later.
    private val _posts = MutableLiveData<List<String>>(emptyList())
    val posts: LiveData<List<String>> = _posts

    fun setPosts(items: List<String>) {
        // TODO Load real liked posts from repository
        _posts.value = items
    }

    fun loadSamplePosts() {
        // Temporary sample data so the UI has something to show.
        _posts.value = listOf("Sample post 1", "Sample post 2", "Sample post 3")
    }
}

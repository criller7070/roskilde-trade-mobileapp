package dk.rosswap.mobile.feature.items.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.items.domain.Post
import dk.rosswap.mobile.R

class SwipeFragment : Fragment() {

    private lateinit var adapter: SwipePostAdapter
    private lateinit var viewPager: ViewPager2
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val posts = mutableListOf<Post>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_swipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager = view.findViewById(R.id.view_pager_posts)
        adapter = SwipePostAdapter()
        viewPager.adapter = adapter

        adapter.setPosts(MockPosts.getMockPosts())
        listenPosts()

        // Button listeners
        view.findViewById<ImageButton>(R.id.btn_skip).setOnClickListener {
            val currentPost = posts.getOrNull(viewPager.currentItem)
            currentPost?.let {
                saveToDisliked(it)
                removePostFromList(it)
                // TODO: Navigate to disliked posts page
            }
        }

        view.findViewById<ImageButton>(R.id.btn_like).setOnClickListener {
            val currentPost = posts.getOrNull(viewPager.currentItem)
            currentPost?.let {
                saveToLiked(it)
                removePostFromList(it)
                // TODO: Navigate to liked posts page
            }
        }
    }

    private fun listenPosts() {
        firestore.collection("posts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                if (snapshot == null || snapshot.isEmpty) {
                    posts.clear()
                    posts.addAll(MockPosts.getMockPosts())
                    adapter.setPosts(posts)
                    return@addSnapshotListener
                }

                val newPosts = snapshot.documents.mapNotNull { doc ->
                    try {
                        Post(
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            imageUrl = doc.getString("imageUrl") ?: "",
                            mode = doc.getString("mode") ?: "",
                            userId = doc.getString("userId") ?: "",
                            userName = doc.getString("userName") ?: "",
                            createdAt = doc.getTimestamp("createdAt")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                posts.clear()
                posts.addAll(newPosts.ifEmpty { MockPosts.getMockPosts() })
                adapter.setPosts(posts)
            }
    }

    private fun saveToLiked(post: Post) {
        // TODO: Save post to Firebase liked collection
        // TODO: firestore.collection("users").document(userId).collection("liked").add(post)
    }

    private fun saveToDisliked(post: Post) {
        // TODO: Save post to Firebase disliked collection
        // TODO: firestore.collection("users").document(userId).collection("disliked").add(post)
    }

    private fun removePostFromList(post: Post) {
        posts.remove(post)
        adapter.setPosts(posts)
    }
}

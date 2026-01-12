package dk.rosswap.mobile.feature.items.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.items.domain.Post
import dk.rosswap.mobile.R
import kotlin.collections.emptyList

class SwipeFragment : Fragment() {

    private lateinit var adapter: SwipePostAdapter
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_swipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val viewPager = view.findViewById<ViewPager2>(R.id.view_pager_posts)
        adapter = SwipePostAdapter()
        viewPager.adapter = adapter

        // Show mocked posts immediately while waiting for Firestore
        adapter.setPosts(MockPosts.getMockPosts())

        listenPosts()
    }

    private fun listenPosts() {
        firestore.collection("posts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                // If snapshot is null or empty, fall back to mocked data
                if (snapshot == null || snapshot.isEmpty) {
                    adapter.setPosts(MockPosts.getMockPosts())
                    return@addSnapshotListener
                }

                val posts = snapshot.documents.mapNotNull { doc ->
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

                // If Firestore returned no valid posts, use mocks
                if (posts.isEmpty()) {
                    adapter.setPosts(MockPosts.getMockPosts())
                } else {
                    adapter.setPosts(posts)
                }
            }
    }
}

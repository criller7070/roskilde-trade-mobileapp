package dk.rosswap.mobile.feature.items.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.TextPaint
import androidx.navigation.fragment.findNavController

import androidx.fragment.app.Fragment
import android.widget.TextView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dk.rosswap.mobile.feature.items.domain.Post
import dk.rosswap.mobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.remove

class SwipeFragment : Fragment() {

    private lateinit var adapter: SwipePostAdapter
    private lateinit var viewPager: ViewPager2
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val posts = mutableListOf<Post>()

    private var startX = 0f
    private var startY = 0f
    private val swipeThreshold = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_swipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewPager = view.findViewById(R.id.view_pager_posts)
        adapter = SwipePostAdapter()
        viewPager.adapter = adapter

        adapter.setPosts(MockPosts.getMockPosts())
        listenPosts()
        setupSwipeListener(view)
        setupButtonListeners(view)
    }

    private fun setupSwipeListener(view: View) {
        viewPager.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.x
                    startY = event.y
                    false
                }
                MotionEvent.ACTION_UP -> {
                    val endX = event.x
                    val endY = event.y
                    val diffX = endX - startX
                    val diffY = endY - startY

                    if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY) &&
                        kotlin.math.abs(diffX) > swipeThreshold
                    ) {
                        val currentPost = posts.getOrNull(viewPager.currentItem)
                        currentPost?.let {
                            when {
                                diffX > 0 -> {
                                    // Swipe right = Like
                                    saveToLiked(it)
                                    removePostFromList(it)
                                }
                                diffX < 0 -> {
                                    // Swipe left = Dislike (TODO)
                                    saveToDisliked(it)
                                    removePostFromList(it)
                                }
                            }
                        }
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }
    }

    private fun setupButtonListeners(view: View) {
        view.findViewById<View>(R.id.btn_skip).setOnClickListener {
            val currentPost = posts.getOrNull(viewPager.currentItem)
            currentPost?.let {
                saveToDisliked(it)
                removePostFromList(it)
            }
        }

        view.findViewById<View>(R.id.btn_like).setOnClickListener {
            val currentPost = posts.getOrNull(viewPager.currentItem)
            currentPost?.let {
                saveToLiked(it)
                removePostFromList(it)
            }
        }

        view.findViewById<View>(R.id.btn_message).setOnClickListener {
            val currentPost = posts.getOrNull(viewPager.currentItem)
            currentPost?.let {
                // TODO: Navigate to messaging page with currentPost
            }
        }
    }

    private fun listenPosts() {
        firestore.collection("items")
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
                            id = doc.id,
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
        val userId = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                firestore.collection("users").document(userId)
                    .update("likedItemIds", FieldValue.arrayUnion(post.id))
                    .addOnSuccessListener {
                        // TODO: Show success message (toast or snackbar)
                    }
                    .addOnFailureListener { e ->
                        // TODO: Show error message
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveToDisliked(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // TODO: Implement disliked logic
                // firestore.collection("users").document(userId)
                //     .update("dislikedItemIds", FieldValue.arrayUnion(post.title))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removePostFromList(post: Post) {
        posts.remove(post)
        adapter.setPosts(posts)

        if (posts.isEmpty()) {
            view?.let { v ->
                val emptyMessageTv = v.findViewById<TextView>(R.id.tv_empty_message)
                val viewPager = v.findViewById<ViewPager2>(R.id.view_pager_posts)

                // Create clickable text
                val text = "You have swiped all of the posts.\nYou can see your liked posts here"
                val spannableString = SpannableString(text)
                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        findNavController().navigate(R.id.nav_liked)
                    }
                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = true
                        ds.color = requireContext().getColor(android.R.color.holo_blue_dark)
                    }
                }

                spannableString.setSpan(clickableSpan, text.length - 4, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                emptyMessageTv.text = spannableString
                emptyMessageTv.movementMethod = LinkMovementMethod.getInstance()
                emptyMessageTv.visibility = View.VISIBLE

                viewPager.visibility = View.GONE
            }
        }
    }

}

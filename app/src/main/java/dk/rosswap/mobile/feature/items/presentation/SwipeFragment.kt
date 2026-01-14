package dk.rosswap.mobile.feature.items.presentation

import android.os.Bundle
import android.view.LayoutInflater
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.StackFrom
import dk.rosswap.mobile.feature.items.domain.Post
import dk.rosswap.mobile.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SwipeFragment : Fragment() {

    private lateinit var cardStackView: CardStackView
    private lateinit var cardStackAdapter: SwipePostAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val posts = mutableListOf<Post>()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_swipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardStackView = view.findViewById(R.id.card_stack_view)
        cardStackAdapter = SwipePostAdapter()

        // Start with mock posts while loading from Firestore
        posts.addAll(MockPosts.getMockPosts())
        cardStackAdapter.setPosts(posts)

        setupCardStack()
        listenPosts()
        setupButtonListeners(view)
    }

    private fun setupCardStack() {
        val listener = object : CardStackListener {
            override fun onCardDragging(direction: Direction, ratio: Float) {
                // Called while card is being dragged
            }

            override fun onCardSwiped(direction: Direction) {
                when (direction) {
                    Direction.Left -> {
                        // Dislike
                        if (posts.isNotEmpty()) {
                            val currentPost = posts[0]
                            saveToDisliked(currentPost)
                            removePostFromList(currentPost)
                        }
                    }
                    Direction.Right -> {
                        // Like
                        if (posts.isNotEmpty()) {
                            val currentPost = posts[0]
                            saveToLiked(currentPost)
                            removePostFromList(currentPost)
                        }
                    }
                    Direction.Top -> {
                        // Message
                        if (posts.isNotEmpty()) {
                            // TODO: Navigate to messaging page with posts[0]
                        }
                    }
                    Direction.Bottom -> {
                        // Not used
                    }
                }
            }

            override fun onCardRewound() {
                // Called when card is returned to original position
            }

            override fun onCardCanceled() {
                // Called when card is not swiped far enough
            }

            override fun onCardAppeared(view: View, position: Int) {
                // Called when a new card appears
            }

            override fun onCardDisappeared(view: View, position: Int) {
                // Called when a card disappears
            }
        }

        val manager = CardStackLayoutManager(requireContext(), listener)
        manager.setStackFrom(StackFrom.None)
        manager.setVisibleCount(1)
        manager.setTranslationInterval(8.0f)
        manager.setScaleInterval(0.95f)
        manager.setMaxDegree(20.0f)
        manager.setDirections(listOf(Direction.Left, Direction.Right, Direction.Top))
        manager.setCanScrollHorizontal(true)
        manager.setCanScrollVertical(true)
        manager.setSwipeThreshold(0.3f)

        cardStackView.layoutManager = manager
        cardStackView.adapter = cardStackAdapter
    }




    private fun setupButtonListeners(view: View) {
        view.findViewById<View>(R.id.btn_skip).setOnClickListener {
            if (posts.isNotEmpty()) {
                val currentPost = posts[0]
                saveToDisliked(currentPost)
                removePostFromList(currentPost)
            }
        }

        view.findViewById<View>(R.id.btn_like).setOnClickListener {
            if (posts.isNotEmpty()) {
                val currentPost = posts[0]
                saveToLiked(currentPost)
                removePostFromList(currentPost)
            }
        }

        view.findViewById<View>(R.id.btn_message).setOnClickListener {
            if (posts.isNotEmpty()) {
                // TODO: Navigate to messaging page with posts[0]
            }
        }
    }

    private fun listenPosts() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get().addOnSuccessListener { userDoc ->
            val seenIds = (userDoc.get("seenItemIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val likedIds = (userDoc.get("likedItemIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val dislikedIds = (userDoc.get("dislikedItemIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            firestore.collection("items")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener

                    val newPosts = snapshot?.documents?.mapNotNull { doc ->
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
                    }?.filter {
                        it.id !in seenIds && it.id !in likedIds && it.id !in dislikedIds && it.userId != userId
                    } ?: emptyList()

                    posts.clear()
                    posts.addAll(newPosts.ifEmpty { MockPosts.getMockPosts().filter { it.userId != userId } })
                    cardStackAdapter.setPosts(posts)
                }
        }
    }

    private fun saveToLiked(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("users").document(userId)
                .update("likedItemIds", FieldValue.arrayUnion(post.id))
        }
    }

    private fun saveToDisliked(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("users").document(userId)
                .update("dislikedItemIds", FieldValue.arrayUnion(post.id))
        }
    }

    private fun removePostFromList(post: Post) {
        val userId = auth.currentUser?.uid ?: return
        posts.remove(post)
        cardStackAdapter.setPosts(posts)

        CoroutineScope(Dispatchers.IO).launch {
            firestore.collection("users").document(userId)
                .update("seenItemIds", FieldValue.arrayUnion(post.id))
        }

        if (posts.isEmpty()) {
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        view?.let { v ->
            val emptyMessageTv = v.findViewById<TextView>(R.id.tv_empty_message)
            val cardStackView = v.findViewById<CardStackView>(R.id.card_stack_view)

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
            cardStackView.visibility = View.GONE
        }
    }
}


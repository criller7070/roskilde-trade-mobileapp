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
import dk.rosswap.mobile.core.utils.GenerateChatIdUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SwipeFragment : Fragment() {

    private lateinit var cardStackView: CardStackView
    private lateinit var cardStackAdapter: SwipePostAdapter
    private lateinit var cardStackLayoutManager: CardStackLayoutManager
    @Volatile private var swipeDirection: Direction? = null
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val posts = mutableListOf<Post>()
    private var likedIds = emptyList<String>()
    private var dislikedIds = emptyList<String>()
    private var allItems = emptyList<Post>()
    private var itemsLoaded = false


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
                // Store the swipe direction to process in onCardDisappeared
                swipeDirection = direction
            }

            override fun onCardRewound() {
                // Called when card is returned to original position
            }

            override fun onCardCanceled() {
                // Called when card is not swiped far enough
                swipeDirection = null
            }

            override fun onCardAppeared(view: View, position: Int) {
                // Called when a new card appears
            }

            override fun onCardDisappeared(view: View?, position: Int) {
                // Called when a card disappears - process the swipe here with the position
                val direction = swipeDirection
                if (direction == null) return
                swipeDirection = null
                
                if (position >= 0 && position < posts.size) {
                    val swipedPost = posts[position]
                    when (direction) {
                        Direction.Left -> {
                            // Dislike
                            saveToDisliked(swipedPost)
                            removePostFromList(swipedPost)
                        }
                        Direction.Right -> {
                            // Like
                            saveToLiked(swipedPost)
                            removePostFromList(swipedPost)
                        }
                        Direction.Top -> {
                            // Message
                            navigateToChat(swipedPost)
                        }
                        Direction.Bottom -> {
                            // Not used
                        }
                    }
                }
            }
        }

        cardStackLayoutManager = CardStackLayoutManager(requireContext(), listener)
        cardStackLayoutManager.setStackFrom(StackFrom.None)
        cardStackLayoutManager.setVisibleCount(1)
        cardStackLayoutManager.setTranslationInterval(8.0f)
        cardStackLayoutManager.setScaleInterval(0.95f)
        cardStackLayoutManager.setMaxDegree(20.0f)
        cardStackLayoutManager.setDirections(listOf(Direction.Left, Direction.Right, Direction.Top))
        cardStackLayoutManager.setCanScrollHorizontal(true)
        cardStackLayoutManager.setCanScrollVertical(true)
        cardStackLayoutManager.setSwipeThreshold(0.3f)

        cardStackView.layoutManager = cardStackLayoutManager
        cardStackView.adapter = cardStackAdapter
    }




    private fun setupButtonListeners(view: View) {
        view.findViewById<View>(R.id.btn_skip).setOnClickListener {
            val topPosition = cardStackLayoutManager.topPosition
            if (topPosition >= 0 && topPosition < posts.size) {
                val currentPost = posts[topPosition]
                saveToDisliked(currentPost)
                removePostFromList(currentPost)
            }
        }

        view.findViewById<View>(R.id.btn_like).setOnClickListener {
            val topPosition = cardStackLayoutManager.topPosition
            if (topPosition >= 0 && topPosition < posts.size) {
                val currentPost = posts[topPosition]
                saveToLiked(currentPost)
                removePostFromList(currentPost)
            }
        }

        view.findViewById<View>(R.id.btn_message).setOnClickListener {
            val topPosition = cardStackLayoutManager.topPosition
            if (topPosition >= 0 && topPosition < posts.size) {
                val currentPost = posts[topPosition]
                navigateToChat(currentPost)
                removePostFromList(currentPost)
            }
        }
    }

    private fun listenPosts() {
        val userId = auth.currentUser?.uid ?: return
        android.util.Log.d("SwipeFragment", "listenPosts started for user: $userId")

        // Load items once at the start
        firestore.collection("items")
            .get()
            .addOnSuccessListener { snapshot ->
                allItems = snapshot.documents.mapNotNull { doc ->
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
                        android.util.Log.e("SwipeFragment", "Error parsing post", e)
                        null
                    }
                }
                
                itemsLoaded = true
                android.util.Log.d("SwipeFragment", "Loaded ${allItems.size} items from Firestore")
                updateFilteredPosts(userId)
            }
            .addOnFailureListener { error ->
                android.util.Log.e("SwipeFragment", "Error loading items", error)
            }

        // Listen to user document in real-time for liked/disliked updates
        firestore.collection("users").document(userId)
            .addSnapshotListener { userDoc, error ->
                if (error != null || userDoc == null) {
                    android.util.Log.e("SwipeFragment", "Error listening to user doc", error)
                    return@addSnapshotListener
                }

                val newLikedIds = (userDoc.get("likedItemIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                val newDislikedIds = (userDoc.get("dislikedItemIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                
                // Only update if the lists actually changed AND items are loaded
                if ((newLikedIds != likedIds || newDislikedIds != dislikedIds) && itemsLoaded) {
                    likedIds = newLikedIds
                    dislikedIds = newDislikedIds
                    
                    android.util.Log.d("SwipeFragment", "User doc updated: liked=${likedIds.size}, disliked=${dislikedIds.size}")

                    // Refresh the posts to apply updated filters
                    updateFilteredPosts(userId)
                }
            }
    }

    private fun updateFilteredPosts(userId: String) {
        val filteredPosts = allItems.filter { post ->
            // Show items that: are not liked, are not disliked, and are not created by current user
            val isNotLiked = post.id !in likedIds
            val isNotDisliked = post.id !in dislikedIds
            val isNotOwnItem = post.userId != userId
            
            isNotLiked && isNotDisliked && isNotOwnItem
        }

        posts.clear()
        posts.addAll(filteredPosts)
        cardStackAdapter.notifyDataSetChanged()
        
        android.util.Log.d("SwipeFragment", "Total items: ${allItems.size}, Filtered: ${filteredPosts.size}, Liked: ${likedIds.size}, Disliked: ${dislikedIds.size}")
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
        posts.remove(post)
        cardStackAdapter.setPosts(posts)

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

            val clickableWord = "here"
            val startIndex = text.lastIndexOf(clickableWord)
            if (startIndex != -1) {
                spannableString.setSpan(
                    clickableSpan,
                    startIndex,
                    startIndex + clickableWord.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            emptyMessageTv.text = spannableString
            emptyMessageTv.movementMethod = LinkMovementMethod.getInstance()
            emptyMessageTv.visibility = View.VISIBLE
            cardStackView.visibility = View.GONE
        }
    }

    private fun navigateToChat(post: Post) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Generate chat ID using the utility function
        val chatIdResult = GenerateChatIdUtil.generate(
            userAId = currentUserId,
            userBId = post.userId,
            itemId = post.id
        )

        chatIdResult.onSuccess { chatId ->
            val bundle = Bundle().apply {
                putString("chatId", chatId)
                putString("itemName", post.title)
                putString("itemImage", post.imageUrl)
            }
            findNavController().navigate(R.id.nav_chatconvo, bundle)
        }.onFailure { error ->
            // Handle error if chat ID generation fails (e.g., trying to chat with self)
            android.util.Log.e("SwipeFragment", "Failed to generate chat ID: ${error.message}")
        }
    }
}


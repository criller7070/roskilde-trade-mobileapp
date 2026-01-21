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
    // Removed swipeDirection as we handle it immediately in onCardSwiped
    // Removed lastSwipedPostId in favor of a set to track all local swipes
    private val locallySwipedIds = mutableSetOf<String>()
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val posts = mutableListOf<Post>()
    private var likedIds = emptyList<String>()
    private var dislikedIds = emptyList<String>()
    private var allItems = emptyList<Post>()
    private var itemsLoaded = false
    private var lastSeenLikedIds = emptySet<String>()
    private var lastSeenDislikedIds = emptySet<String>()
    private var hasFilteredOnce = false
    private var userDocListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null


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

    override fun onDestroyView() {
        super.onDestroyView()
        userDocListenerRegistration?.remove()
        userDocListenerRegistration = null
    }

    private fun setupCardStack() {
        val listener = object : CardStackListener {
            override fun onCardDragging(direction: Direction, ratio: Float) {
                // Called while card is being dragged
            }

            override fun onCardSwiped(direction: Direction) {
                // Use topPosition - 1 because the manager has already advanced to the next card
                val position = cardStackLayoutManager.topPosition - 1
                
                if (position >= 0 && position < posts.size) {
                    val swipedPost = posts[position]
                    locallySwipedIds.add(swipedPost.id)
                    
                    // We do NOT remove the item from the adapter here anymore.
                    // Modifying the adapter list during the swipe animation sequence causes
                    // index issues. The CardStackView handles the visual removal.
                    // The post will be filtered out on the next data refresh because of locallySwipedIds.
                    
                    when (direction) {
                        Direction.Left -> {
                            // Dislike
                            saveToDisliked(swipedPost)
                        }
                        Direction.Right -> {
                            // Like
                            saveToLiked(swipedPost)
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

            override fun onCardRewound() {
                // Called when card is returned to original position
            }

            override fun onCardCanceled() {
                // Called when card is not swiped far enough
            }

            override fun onCardAppeared(view: View, position: Int) {
                // Called when a new card appears
            }

            override fun onCardDisappeared(view: View?, position: Int) {
                // Logic moved to onCardSwiped to ensure correct item is targeted
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
                // For manual buttons, we still need to manage the list manually 
                // or preferably trigger the swipe animation. 
                // Sticking to manual removal for now but adding to local tracking.
                locallySwipedIds.add(currentPost.id)
                posts.removeAt(topPosition)
                cardStackAdapter.notifyItemRemoved(topPosition)
                saveToDisliked(currentPost)
            }
        }

        view.findViewById<View>(R.id.btn_like).setOnClickListener {
            val topPosition = cardStackLayoutManager.topPosition
            if (topPosition >= 0 && topPosition < posts.size) {
                val currentPost = posts[topPosition]
                locallySwipedIds.add(currentPost.id)
                posts.removeAt(topPosition)
                cardStackAdapter.notifyItemRemoved(topPosition)
                saveToLiked(currentPost)
            }
        }

        view.findViewById<View>(R.id.btn_message).setOnClickListener {
            val topPosition = cardStackLayoutManager.topPosition
            if (topPosition >= 0 && topPosition < posts.size) {
                val currentPost = posts[topPosition]
                locallySwipedIds.add(currentPost.id)
                posts.removeAt(topPosition)
                cardStackAdapter.notifyItemRemoved(topPosition)
                navigateToChat(currentPost)
            }
        }
    }

    private fun listenPosts() {
        val userId = auth.currentUser?.uid ?: return
        android.util.Log.d("SwipeFragment", "listenPosts started for user: $userId")
        
        // Reset flags for this fragment lifecycle
        itemsLoaded = false
        hasFilteredOnce = false

        // Load both items and user data in parallel
        firestore.collection("items")
            .get()
            .addOnSuccessListener { itemsSnapshot ->
                allItems = itemsSnapshot.documents.mapNotNull { doc ->
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
                
                android.util.Log.d("SwipeFragment", "Loaded ${allItems.size} items from Firestore")
                itemsLoaded = true
                
                // Now that items are loaded, update filtering with current user data
                updateFilteredPosts(userId)
            }
            .addOnFailureListener { error ->
                android.util.Log.e("SwipeFragment", "Error loading items", error)
            }

        // Also fetch current user data to ensure we have latest likes/dislikes
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { userDoc ->
                if (userDoc != null) {
                    likedIds = (userDoc.get("likedItemIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    dislikedIds = (userDoc.get("dislikedItemIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    
                    android.util.Log.d("SwipeFragment", "User initial data: liked=${likedIds.size}, disliked=${dislikedIds.size}")
                    
                    // If items already loaded, update now
                    if (itemsLoaded) {
                        updateFilteredPosts(userId)
                    }
                }
            }

        // Then listen to user document for real-time updates
        userDocListenerRegistration?.remove()
        userDocListenerRegistration = firestore.collection("users").document(userId)
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
        val currentLikedSet = likedIds.toSet()
        val currentDislikedSet = dislikedIds.toSet()
        
        // Skip update only if we've already filtered AND the lists haven't changed
        // We removed the 'hasFilteredOnce' check constraint slightly because we also want to filter out local swipes
        // but to avoid flickering, we can keep the logic but include local checks.
        
        // Actually, we should allow update if locallySwipedIds changed, but that happens often. 
        // For simplicity, we just filter every time this method is called properly.
        
        hasFilteredOnce = true
        lastSeenLikedIds = currentLikedSet
        lastSeenDislikedIds = currentDislikedSet
        
        val filteredPosts = allItems.filter { post ->
            // Show items that: are not liked, are not disliked, are not created by current user, 
            // and were not just swiped (avoid re-adding)
            val isNotLiked = post.id !in currentLikedSet
            val isNotDisliked = post.id !in currentDislikedSet
            val isNotOwnItem = post.userId != userId
            val wasNotSwipedLocally = post.id !in locallySwipedIds
            
            isNotLiked && isNotDisliked && isNotOwnItem && wasNotSwipedLocally
        }

        // Only update if the filtered list is different
        val currentPostIds = posts.map { it.id }
        val newPostIds = filteredPosts.map { it.id }
        
        if (newPostIds != currentPostIds) {
            posts.clear()
            posts.addAll(filteredPosts)
            cardStackAdapter.notifyDataSetChanged()
            android.util.Log.d("SwipeFragment", "Adapter updated, new filtered count: ${filteredPosts.size}")
        }
        
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

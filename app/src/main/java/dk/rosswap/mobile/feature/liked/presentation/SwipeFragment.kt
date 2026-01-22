package dk.rosswap.mobile.feature.liked.presentation

import android.content.Context
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.yuyakaido.android.cardstackview.CardStackLayoutManager
import com.yuyakaido.android.cardstackview.CardStackListener
import com.yuyakaido.android.cardstackview.CardStackView
import com.yuyakaido.android.cardstackview.Direction
import com.yuyakaido.android.cardstackview.StackFrom
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.model.Item
import dk.rosswap.mobile.core.utils.GenerateChatIdUtil
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SwipeFragment : Fragment() {

    private lateinit var cardStackView: CardStackView
    private lateinit var cardStackAdapter: SwipeAdapter
    private lateinit var cardStackLayoutManager: CardStackLayoutManager

    private val swipeViewModel: SwipeViewModel by viewModels()

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
        cardStackAdapter = SwipeAdapter(onClick = { item -> navigateToChat(item) })

        setupCardStack()
        setupButtonListeners(view)

        // observe posts from ViewModel
        lifecycleScope.launch {
            swipeViewModel.posts.collectLatest { posts ->
                cardStackAdapter.setPosts(posts)
                if (posts.isEmpty()) {
                    showEmptyState()
                } else {
                    view.findViewById<CardStackView>(R.id.card_stack_view).visibility = View.VISIBLE
                    view.findViewById<TextView>(R.id.tv_empty_message).visibility = View.GONE
                }
            }
        }

        // observe errors and loading if needed
        lifecycleScope.launch {
            swipeViewModel.error.collectLatest { errorMsg ->
                if (!errorMsg.isNullOrBlank()) {
                    swipeViewModel.clearError()
                }
            }
        }

        swipeViewModel.start()
    }

    private fun setupCardStack() {
        // the main issue here is that one some emulators, swiping direction are mirrored
        // for whatever reason. Some are RTL - right to left - and some are inverted
        // 1. so for that reason, we get prefs here:
        val isRtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val prefs = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val overrideInvert = prefs.getBoolean("pref_swipe_invert", false)
        val effectiveIsRtl = if (overrideInvert) !isRtl else isRtl

        val listener = object : CardStackListener {
            override fun onCardDragging(direction: Direction, ratio: Float) {
                // called while card is being dragged; wait it out
            }

            override fun onCardSwiped(direction: Direction) {
                val position = cardStackLayoutManager.topPosition - 1

                val posts = cardStackAdapter.currentList
                if (position >= 0 && position < posts.size) {
                    val swipedPost = posts[position]

                    // map physical direction to semantic action based on RTL. Might seem
                    // patchy, but this is entirely valid for now
                    when (direction) {
                        Direction.Left -> if (effectiveIsRtl) swipeViewModel.like(swipedPost) else swipeViewModel.dislike(swipedPost)
                        Direction.Right -> if (effectiveIsRtl) swipeViewModel.dislike(swipedPost) else swipeViewModel.like(swipedPost)
                        Direction.Top -> navigateToChat(swipedPost)
                        Direction.Bottom -> {}
                    }
                }
            }

            override fun onCardRewound() {}
            override fun onCardCanceled() {}
            override fun onCardAppeared(view: View, position: Int) {}
            override fun onCardDisappeared(view: View?, position: Int) {}
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
            val posts = cardStackAdapter.currentList
            if (topPosition >= 0 && topPosition < posts.size) {
                val currentPost = posts[topPosition]
                swipeViewModel.dislike(currentPost)
            }
        }

        view.findViewById<View>(R.id.btn_like).setOnClickListener {
            val topPosition = cardStackLayoutManager.topPosition
            val posts = cardStackAdapter.currentList
            if (topPosition >= 0 && topPosition < posts.size) {
                val currentPost = posts[topPosition]
                swipeViewModel.like(currentPost)
            }
        }

        view.findViewById<View>(R.id.btn_message).setOnClickListener {
            val topPosition = cardStackLayoutManager.topPosition
            val posts = cardStackAdapter.currentList
            if (topPosition >= 0 && topPosition < posts.size) {
                val currentPost = posts[topPosition]
                navigateToChat(currentPost)
            }
        }
    }

    private fun showEmptyState() {
        view?.let { v ->
            val emptyMessageTv = v.findViewById<TextView>(R.id.tv_empty_message)
            val cardStackView = v.findViewById<CardStackView>(R.id.card_stack_view)

            val text = getString(R.string.swipe_empty_message)
            val spannableString = SpannableString(text)
            val clickableSpan = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // ensure user is authenticated before nav to liked
                    val currentUserId = swipeViewModel.currentUserId()
                    if (currentUserId == null) {
                        findNavController().navigate(R.id.nav_login_required)
                    } else {
                        findNavController().navigate(R.id.nav_liked)
                    }
                }
                override fun updateDrawState(ds: TextPaint) {
                    super.updateDrawState(ds)
                    ds.isUnderlineText = true
                    ds.color = requireContext().getColor(android.R.color.holo_blue_dark)
                }
            }

            val clickableWord = getString(R.string.swipe_empty_link_text)
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

    private fun navigateToChat(post: Item) {
        val currentUserId = swipeViewModel.currentUserId() ?: return

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
        }.onFailure {
        }
    }
}
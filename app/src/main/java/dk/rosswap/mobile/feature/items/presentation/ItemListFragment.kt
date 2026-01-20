package dk.rosswap.mobile.feature.items.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentWallBinding
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dk.rosswap.mobile.core.utils.toDetailBundle
import dk.rosswap.mobile.core.common.SessionManager

@AndroidEntryPoint
class ItemListFragment : Fragment() {

    private var _binding: FragmentWallBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemListViewModel by viewModels()

    @Inject lateinit var sessionManager: SessionManager
    @Inject lateinit var chatRepository: ChatRepository

    private lateinit var adapter: ItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWallBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ItemsAdapter(
            onItemClicked = { item ->
                // Navigate to item detail screen with item fields
                findNavController().navigate(R.id.action_nav_wall_to_itemDetail, item.toDetailBundle())
            },
            onMessageClicked = { item ->
                val currentUserId = sessionManager.currentUserId()
                if (currentUserId == null) {
                    lifecycleScope.launch { PopupBus.showError("You must be logged in to message") }
                    return@ItemsAdapter
                }

                val otherUserId = item.userId

                if (currentUserId == otherUserId) {
                    lifecycleScope.launch { PopupBus.showError("You can't message yourself") }
                    return@ItemsAdapter
                }

                lifecycleScope.launch {
                    val result = chatRepository.openChat(
                        currentUserId = currentUserId,
                        otherUserId = otherUserId,
                        itemId = item.id,
                        itemName = item.title,
                        itemImage = item.imageUrl,
                        currentUserName = (sessionManager.authState.value as? dk.rosswap.mobile.core.common.AuthState.Authenticated)?.user?.name?.trim().orEmpty(),
                        otherUserName = item.userName
                    )

                    result.fold(
                        onSuccess = { chatId ->
                            findNavController().navigate(
                                R.id.nav_chatconvo, Bundle().apply {
                                    putString("chatId", chatId)
                                    putString("itemName", item.title) // pass the item title so ChatPageFragment can show it
                                }
                            )
                        },
                        onFailure = { e ->
                            PopupBus.showError(e.message ?: "Could not start chat")
                        }
                    )
                }
            },
            onLikeClicked = { item ->
                viewModel.likeItem(item)
                lifecycleScope.launch {
                    PopupBus.showSuccess("Added to Liked Posts")
                }
            },
            onDislikeClicked = { item ->
                viewModel.dislikeItem(item)
                lifecycleScope.launch {
                    PopupBus.showSuccess("Added to Disliked Posts")
                }
            },
            // Provide auth status so the adapter doesn't toggle UI for unauthenticated users
            isLoggedIn = { sessionManager.currentUserId() != null },
            // Provide current user id so adapter can treat own posts as non-interactive
            currentUserIdProvider = { sessionManager.currentUserId() },
            // Provide liked state from ViewModel so it persists across navigation
            isLikedProvider = { itemId -> viewModel.isLiked(itemId) }
        )

        binding.recyclerPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPosts.adapter = adapter

        // Navigate to the Disliked page when the bottom button is pressed.
        binding.btnDisliked.setOnClickListener {
            // action_nav_wall_to_dislikedFragment was removed; navigate directly to nav_disliked
            findNavController().navigate(R.id.nav_disliked)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.recyclerPosts.isEnabled = !isLoading
            binding.btnDisliked.isEnabled = !isLoading
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { msg ->
            if (!msg.isNullOrBlank()) {
                lifecycleScope.launch {
                    PopupBus.showError(msg)
                }
            }
        }

        viewModel.items.observe(viewLifecycleOwner) { items ->
            adapter.submitList(items)
            val highlightId = arguments?.getString("highlightItemId")
            if (!highlightId.isNullOrBlank()) {
                val idx = items.indexOfFirst { it.id == highlightId }
                if (idx >= 0) binding.recyclerPosts.scrollToPosition(idx)
            }
        }

        // Observe liked items state changes and refresh adapter to update UI
        viewModel.likedItemIds.observe(viewLifecycleOwner) {
            // Notify adapter that data has changed so it can update the like icons
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

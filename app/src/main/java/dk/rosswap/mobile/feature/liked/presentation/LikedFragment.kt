package dk.rosswap.mobile.feature.liked.presentation

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentLikedBinding
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject
import dk.rosswap.mobile.core.common.SessionManager

@AndroidEntryPoint
class LikedFragment : Fragment(R.layout.fragment_liked) {

    private var _binding: FragmentLikedBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LikedViewModel by viewModels()
    private lateinit var adapter: LikedItemAdapter

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var chatRepository: ChatRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentLikedBinding.bind(view)

        setupRecyclerView()
        observeData()

        binding.btnDisliked.setOnClickListener {
            // Navigate to the Disliked screen
            findNavController().navigate(R.id.nav_disliked)
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure we refresh data when the screen becomes visible,
        // catching any new likes from the feed.
        viewModel.loadLikedPosts()
    }

    private fun setupRecyclerView() {
        adapter = LikedItemAdapter(
            onItemClick = { likedItem ->
                // Open chat with the item's owner (same flow as ItemListFragment)
                val item = likedItem.item
                val currentUserId = sessionManager.currentUserId()

                when {
                    currentUserId == null -> lifecycleScope.launch { PopupBus.showError("You must be logged in to message") }
                    item.userId.isBlank() -> lifecycleScope.launch { PopupBus.showError("Missing item owner") }
                    currentUserId == item.userId -> lifecycleScope.launch { PopupBus.showError("You can’t message yourself") }
                    else -> {
                        // All checks passed — open chat
                        lifecycleScope.launch {
                            val result = chatRepository.openChat(
                                currentUserId = currentUserId,
                                otherUserId = item.userId,
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
                                            putString("itemName", item.title)
                                        }
                                    )
                                },
                                onFailure = { e ->
                                    PopupBus.showError(e.message ?: "Could not start chat")
                                }
                            )
                        }
                    }
                }
            },
            onUnlikeClick = { likedItem ->
                viewModel.unlikePost(likedItem)
            }
        )

        binding.recyclerPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPosts.adapter = adapter
    }

    private fun observeData() {
        viewModel.likedPosts.observe(viewLifecycleOwner) { posts ->
            adapter.submitList(posts)
            binding.emptyStateGroup.isVisible = posts.isEmpty()
            binding.recyclerPosts.isVisible = posts.isNotEmpty()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

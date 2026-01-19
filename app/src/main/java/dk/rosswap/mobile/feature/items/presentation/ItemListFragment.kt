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
import com.google.firebase.auth.FirebaseAuth
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentWallBinding
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.feature.chat.domain.ChatRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dk.rosswap.mobile.core.utils.toDetailBundle

@AndroidEntryPoint
class ItemListFragment : Fragment() {

    private var _binding: FragmentWallBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ItemListViewModel by viewModels()

    @Inject lateinit var auth: FirebaseAuth
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
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    lifecycleScope.launch { PopupBus.showError("You must be logged in to message") }
                    return@ItemsAdapter
                }

                if (item.userId.isBlank()) {
                    lifecycleScope.launch { PopupBus.showError("Missing item owner") }
                    return@ItemsAdapter
                }

                val currentUserId = currentUser.uid
                val otherUserId = item.userId

                if (currentUserId == otherUserId) {
                    lifecycleScope.launch { PopupBus.showError("You can’t message yourself") }
                    return@ItemsAdapter
                }

                lifecycleScope.launch {
                    val result = chatRepository.openChat(
                        currentUserId = currentUserId,
                        otherUserId = otherUserId,
                        itemId = item.id,
                        itemName = item.title,
                        itemImage = item.imageUrl,
                        currentUserName = currentUser.displayName?.trim().orEmpty(),
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
            }
        )

        binding.recyclerPosts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerPosts.adapter = adapter

        // Navigate to the Disliked page when the bottom button is pressed.
        binding.btnDisliked.setOnClickListener {
            // action_nav_wall_to_dislikedFragment was removed; navigate directly to nav_disliked
            findNavController().navigate(R.id.nav_disliked)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.root.isEnabled = !isLoading
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
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

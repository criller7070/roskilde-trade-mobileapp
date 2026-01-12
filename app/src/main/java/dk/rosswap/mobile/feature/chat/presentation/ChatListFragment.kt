package dk.rosswap.mobile.feature.chat.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.databinding.FragmentChatListBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatListViewModel by viewModels()

    private val adapter = ChatListAdapter(
        onChatClick = { userChat ->
            // Ensure itemName and itemImage are passed so ChatPageFragment can show a friendly title
            val args = bundleOf(
                "chatId" to userChat.id,
                "itemName" to (userChat.itemName ?: ""),
                "itemImage" to (userChat.itemImage ?: "")
            )
            findNavController().navigate(
                R.id.action_nav_chat_list_to_nav_chatconvo,
                args
            )
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerMessages.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessages.adapter = adapter

        fun updateEmptyLoading(chats: List<*>, isLoading: Boolean) {
            binding.tvLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.tvEmpty.visibility = if (!isLoading && chats.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            val currentChats = viewModel.chats.value.orEmpty()
            updateEmptyLoading(currentChats, isLoading)
        }

        viewModel.chats.observe(viewLifecycleOwner) { chats ->
            adapter.submit(chats)
            updateEmptyLoading(chats, viewModel.isLoading.value == true)
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (err != null) {
                lifecycleScope.launch {
                    PopupBus.showError(err.message ?: "Chat fejl")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

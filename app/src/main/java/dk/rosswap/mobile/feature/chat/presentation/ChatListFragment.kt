package dk.rosswap.mobile.feature.chat.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentChatListBinding

@AndroidEntryPoint
class ChatListFragment : Fragment() {

    private var _binding: FragmentChatListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatListViewModel by viewModels()

    private val adapter = ChatListAdapter(
        onChatClick = { userChat ->
            findNavController().navigate(
                R.id.action_nav_chat_list_to_nav_chatconvo,
                bundleOf("chatId" to userChat.id)
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

        viewModel.chats.observe(viewLifecycleOwner) { chats ->
            adapter.submit(chats)
        }

        // For now we just stop updating the list on error; later we can use PopupBus.
        viewModel.error.observe(viewLifecycleOwner) {
            // no-op UI for now
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

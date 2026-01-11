package dk.rosswap.mobile.feature.chat.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.databinding.FragmentChatPageBinding

@AndroidEntryPoint
class ChatPageFragment : Fragment() {

    private var _binding: FragmentChatPageBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatPageViewModel by viewModels()

    private val adapter by lazy {
        ChatMessageAdapter(currentUserId = { viewModel.currentUserId() })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chatId = arguments?.getString("chatId").orEmpty()
        if (chatId.isNotBlank()) {
            binding.chatTitle.text = "Chat: $chatId"
            viewModel.startObserving(chatId)
        }

        binding.recyclerMessagesReceived.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessagesReceived.adapter = adapter
        binding.recyclerMessagesSent.visibility = View.GONE

        viewModel.messages.observe(viewLifecycleOwner) { msgs ->
            adapter.submit(msgs)
            // Autoscroll like the web app
            if (msgs.isNotEmpty()) {
                binding.recyclerMessagesReceived.scrollToPosition(msgs.size - 1)
            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
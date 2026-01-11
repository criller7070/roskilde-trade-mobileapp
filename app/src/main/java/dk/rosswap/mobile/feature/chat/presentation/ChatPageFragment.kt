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

    private var initialScrollDone = false
    private var currentChatId: String = ""

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

        currentChatId = arguments?.getString("chatId").orEmpty()
        if (currentChatId.isNotBlank()) {
            binding.chatTitle.text = "Chat: $currentChatId"
            viewModel.startObserving(currentChatId)
        }

        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessagesReceived.layoutManager = layoutManager
        binding.recyclerMessagesReceived.adapter = adapter
        binding.recyclerMessagesSent.visibility = View.GONE

        viewModel.messages.observe(viewLifecycleOwner) { msgs ->
            binding.emptyPlaceholder.visibility = if (msgs.isEmpty()) View.VISIBLE else View.GONE

            val lastVisible = layoutManager.findLastVisibleItemPosition()
            val shouldAutoScroll = !initialScrollDone || lastVisible >= adapter.itemCount - 2

            adapter.submit(msgs)

            if (msgs.isNotEmpty() && shouldAutoScroll) {
                binding.recyclerMessagesReceived.scrollToPosition(msgs.size - 1)
                initialScrollDone = true
            } else if (msgs.isNotEmpty()) {
                initialScrollDone = true
            }
        }

        viewModel.isSending.observe(viewLifecycleOwner) { sending ->
            binding.btnSend.isEnabled = !sending
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            if (currentChatId.isBlank()) return@setOnClickListener

            viewModel.sendMessage(currentChatId, text) {
                binding.etMessage.setText("")
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
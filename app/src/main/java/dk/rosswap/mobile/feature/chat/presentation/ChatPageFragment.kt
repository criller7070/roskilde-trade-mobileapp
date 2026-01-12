package dk.rosswap.mobile.feature.chat.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentChatPageBinding
import kotlinx.coroutines.launch

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

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes() ?: return@let
                inputStream.close()

                val fileName = "${System.currentTimeMillis()}.jpg"
                viewModel.sendImageMessage(currentChatId, fileName, bytes) {
                    // Success callback
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to read image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
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

        currentChatId = arguments?.getString("chatId").orEmpty()
        val itemName = arguments?.getString("itemName").orEmpty()
        val itemImageArg = arguments?.getString("itemImage").orEmpty()

        if (currentChatId.isNotBlank()) {
            binding.chatTitle.text = itemName.takeIf { it.isNotBlank() } ?: "Chat"
            viewModel.startObserving(currentChatId)
        }

        // Load item preview if an image was provided via nav args (resolve Firebase storage refs)
        if (!itemImageArg.isNullOrBlank()) {
            loadImageStringIntoPreview(itemImageArg)
        } else {
            // hide preview if none
            binding.itemPreview.setImageResource(R.drawable.ic_photo_placeholder)
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

        viewModel.isUploadingImage.observe(viewLifecycleOwner) { uploading ->
            binding.btnCamera.isEnabled = !uploading
        }

        viewModel.rateLimitError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.imageUploadError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            if (currentChatId.isBlank()) return@setOnClickListener

            viewModel.sendMessage(currentChatId, text) {
                binding.etMessage.setText("")
            }
        }

        binding.btnCamera.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadImageStringIntoPreview(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return

        // If it's already an http(s) url -> load directly
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            binding.itemPreview.load(trimmed) {
                placeholder(R.drawable.ic_photo_placeholder)
                error(R.drawable.ic_photo_placeholder)
            }
            return
        }

        // If it's a gs:// url or storage path, resolve with Firebase Storage
        val storage = FirebaseStorage.getInstance()
        try {
            val ref = if (trimmed.startsWith("gs://")) storage.getReferenceFromUrl(trimmed) else storage.reference.child(trimmed)
            ref.downloadUrl
                .addOnSuccessListener { uri ->
                    binding.itemPreview.load(uri.toString()) {
                        placeholder(R.drawable.ic_photo_placeholder)
                        error(R.drawable.ic_photo_placeholder)
                    }
                }
                .addOnFailureListener { e ->
                    // fallback to placeholder
                    binding.itemPreview.setImageResource(R.drawable.ic_photo_placeholder)
                }
        } catch (e: Exception) {
            binding.itemPreview.setImageResource(R.drawable.ic_photo_placeholder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
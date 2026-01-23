package dk.rosswap.mobile.feature.chat.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentChatPageBinding
import dk.rosswap.mobile.core.utils.GetFileExtensionUtil

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

    // image picker
    // returns a Uri that we convert to bytes and upload
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes() ?: return@let
                inputStream.close()

                // here we use GetFileExtensionUtil at the UI layer
                val ext = try {
                    GetFileExtensionUtil.getFileExtension(requireContext(), it)
                } catch (_: Exception) {
                    "jpg"
                }

                val fileName = "${System.currentTimeMillis()}.$ext"
                viewModel.sendImageMessage(currentChatId, fileName, bytes) {
                    // success callback
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

        // nav args
        currentChatId = arguments?.getString("chatId").orEmpty()
        val itemName = arguments?.getString("itemName").orEmpty()
        val itemImageArg = arguments?.getString("itemImage").orEmpty()

        if (currentChatId.isNotBlank()) {
            // prefer explicit null/blank check for title
            binding.chatTitle.text = itemName.ifBlank { "Chat" }
            // start observing messages for this chat
            viewModel.startObserving(currentChatId)
        }

        // load item preview if an image was provided via nav args (resolve Firebase storage refs)
        if (itemImageArg.isNotBlank()) {
            loadImageStringIntoPreview(itemImageArg)
        } else {
            // hide preview if none
            binding.itemPreview.setImageResource(R.drawable.ic_photo_placeholder)
        }

        // RecyclerView setup
        val layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerMessagesReceived.layoutManager = layoutManager
        binding.recyclerMessagesReceived.adapter = adapter
        binding.recyclerMessagesSent.visibility = View.GONE

        // observe messages and auto-scroll when appropriate
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

        // toggle send button while sending
        viewModel.isSending.observe(viewLifecycleOwner) { sending ->
            binding.btnSend.isEnabled = !sending
        }

        // toggle camera while uploading image
        viewModel.isUploadingImage.observe(viewLifecycleOwner) { uploading ->
            binding.btnCamera.isEnabled = !uploading
        }

        // show rate limit and upload errors as toasts
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

        // send button
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text?.toString().orEmpty()
            if (currentChatId.isBlank()) return@setOnClickListener

            viewModel.sendMessage(currentChatId, text) {
                binding.etMessage.setText("")
            }
        }

        // image picker again
        binding.btnCamera.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // Remove the visible back-arrow button from the chat conversation UI.
        // we only hide the button (no navigation logic is changed).
        binding.btnBack.visibility = View.GONE
    }

    // little util to load an image string into the preview
    private fun loadImageStringIntoPreview(raw: String) {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return

        // If it's already an http url, load directly
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
                .addOnFailureListener { _ ->
                    // fallback to placeholder
                    binding.itemPreview.setImageResource(R.drawable.ic_photo_placeholder)
                }
        } catch (_: Exception) {
            binding.itemPreview.setImageResource(R.drawable.ic_photo_placeholder)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
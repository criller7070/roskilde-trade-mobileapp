package dk.rosswap.mobile.feature.items.presentation

import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.databinding.FragmentAddItemBinding

@AndroidEntryPoint
class AddItemFragment : Fragment(R.layout.fragment_add_item) {
    // binding
    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddItemViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    // image picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPostImage.setImageURI(uri)
            binding.layoutUploadPlaceholder.isVisible = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddItemBinding.bind(view)

        // 1. Image Picker
        binding.cardImageUpload.setOnClickListener {
            pickImage.launch("image/*")
        }

        // 2. Character Counters
        binding.etTitle.addTextChangedListener { text ->
            val count = text?.length ?: 0
            binding.tvTitleCount.text = getString(R.string.items_title_count, count)
        }

        binding.etDescription.addTextChangedListener { text ->
            val count = text?.length ?: 0
            binding.tvDescCount.text = getString(R.string.items_description_count, count)
        }

        // 3. Create Button
        binding.btnCreatePost.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            // Get selected mode (Bytte/Sælge)
            val isSelling = binding.toggleType.checkedButtonId == R.id.btn_sell
            val type = if (isSelling) "sælge" else "bytte"

            // If any required field is missing, show a single combined message
            val missingTitle = title.isBlank()
            val missingDescription = description.isBlank()
            val missingImage = selectedImageUri == null

            if (missingTitle || missingDescription || missingImage) {
                PopupBus.showError("Please fill in all fields and choose an image.")
                return@setOnClickListener
            }

            viewModel.createPost(title, description, selectedImageUri, type)
        }

        observeViewModel()
    }

    // form = collection of UI elements that interact with the user
    // and should be easily toggled in one go
    private fun setFormEnabled(enabled: Boolean) {
        binding.btnCreatePost.isEnabled = enabled
        binding.etTitle.isEnabled = enabled
        binding.etDescription.isEnabled = enabled
        binding.toggleType.isEnabled = enabled
        binding.cardImageUpload.isEnabled = enabled
    }

    // form is reset when a new post is created
    private fun resetForm() {
        selectedImageUri = null
        binding.etTitle.setText("")
        binding.etDescription.setText("")
        binding.ivPostImage.setImageDrawable(null)
        binding.layoutUploadPlaceholder.isVisible = true
        // Reset counts
        binding.tvTitleCount.text = getString(R.string.items_title_count, 0)
        binding.tvDescCount.text = getString(R.string.items_description_count, 0)
    }

    // observe ViewModel for...
    private fun observeViewModel() {
        // ... loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            setFormEnabled(!isLoading)
        }

        // ... create post
        viewModel.createResult.observe(viewLifecycleOwner) { result ->
            if (result.isSuccess) {
                PopupBus.showSuccess("Post created successfully.")
                resetForm()
                findNavController().navigate(R.id.nav_home)
            } else {
                val msg = result.exceptionOrNull()?.message?.takeIf { it.isNotBlank() }
                    ?: "Failed to create post"
                PopupBus.showError(msg)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

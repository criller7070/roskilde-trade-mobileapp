package dk.rosswap.mobile.feature.items.presentation

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentCreatePostBinding

@AndroidEntryPoint
class AddItemFragment : Fragment(R.layout.fragment_create_post) {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddItemViewModel by viewModels()
    private var selectedImageUri: Uri? = null

    // Helper to open gallery
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivPostImage.setImageURI(uri)
            binding.layoutUploadPlaceholder.isVisible = false // Hide the placeholder icon/text
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCreatePostBinding.bind(view)

        // 1. Image Picker
        binding.cardImageUpload.setOnClickListener {
            pickImage.launch("image/*")
        }

        // 2. Character Counters
        binding.etTitle.addTextChangedListener { text ->
            val count = text?.length ?: 0
            binding.tvTitleCount.text = "$count/60 characters"
        }

        binding.etDescription.addTextChangedListener { text ->
            val count = text?.length ?: 0
            binding.tvDescCount.text = "$count/500 characters"
        }

        // 3. Create Button
        binding.btnCreatePost.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()

            // Get selected type (Trade/Sell)
            val isSelling = binding.toggleType.checkedButtonId == R.id.btn_sell
            val type = if (isSelling) "sell" else "trade"

            if (title.isNotEmpty() && description.isNotEmpty()) {
                // If you want to enforce image upload, check (selectedImageUri != null) here
                viewModel.createPost(title, description, selectedImageUri, type)
            } else {
                Toast.makeText(requireContext(), "Please add a title and description", Toast.LENGTH_SHORT).show()
            }
        }

        // 4. Observe State (Loading/Success)
        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            // Assume you added a progressBar to the layout, or just disable button
            binding.btnCreatePost.isEnabled = !isLoading
            // If you added a progressBar in XML: binding.progressBar.isVisible = isLoading
        }

        viewModel.postCreated.observe(viewLifecycleOwner) { success ->
            when (success) {
                true -> {
                    Toast.makeText(requireContext(), "Post Created Successfully!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.nav_home)
                }
                false -> {
                    Toast.makeText(requireContext(), "Failed to create post", Toast.LENGTH_SHORT).show()
                }
                null -> {
                    // Ignore initial or non-result emissions
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

package dk.rosswap.mobile.core.ui.reportbug

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dk.rosswap.mobile.databinding.FragmentReportBugBinding

class ReportBugFragment : Fragment() {

    private var _binding: FragmentReportBugBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportBugViewModel by viewModels()

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            viewModel.setImage(uri)
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBugBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // Description input
        binding.descriptionInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val text = s.toString()
                viewModel.onDescriptionChanged(text)
                binding.charCounter.text = "${text.length}/1000"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Image picker click
        binding.imageUploadContainer.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // Observe image selection
        viewModel.imageUri.observe(viewLifecycleOwner) { uri ->
            uri?.let {
                binding.uploadIcon.setImageURI(it)
            }
        }

        // Submit button state
        viewModel.isSubmitEnabled.observe(viewLifecycleOwner) {
            binding.submitButton.isEnabled = it
        }

        // Submit click
        binding.submitButton.setOnClickListener {
            viewModel.submitBugReport()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

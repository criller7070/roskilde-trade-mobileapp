package dk.rosswap.mobile.feature.bugreport.presentation

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.databinding.FragmentBugReportBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class ReportBugFragment : Fragment() {

    private var _binding: FragmentBugReportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BugReportViewModel by viewModels()

    // 🔹 HOLDS SELECTED IMAGE
    private var selectedImageUri: Uri? = null

    // 🔹 DEFAULT UPLOAD ICON
    private val defaultUploadIconRes = android.R.drawable.ic_menu_camera

    // 🔹 IMAGE PICKER
    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                binding.uploadIcon.setImageURI(uri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBugReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔹 AUTO INCLUDED INFO (LIKE WEB)
        val user = FirebaseAuth.getInstance().currentUser
        val userLabel = user?.displayName ?: user?.email ?: "Anonymous"
        val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL}"
        val timestamp = SimpleDateFormat(
            "dd.MM.yyyy HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

        binding.autoInfoDetails.text = """
• Your account: $userLabel
• Device: $deviceInfo (Android ${Build.VERSION.RELEASE})
• Timestamp: $timestamp
""".trimIndent()

        // 🔹 CHARACTER COUNTER + ENABLE SUBMIT
        binding.descriptionInput.addTextChangedListener {
            val rawText = it?.toString().orEmpty()
            val trimmedText = rawText.trim()
            binding.charCounter.text = "${rawText.length} / 1000"
            binding.submitButton.isEnabled = trimmedText.isNotEmpty()
        }

        // 🔹 IMAGE PICKER CLICK
        binding.imageUploadContainer.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // 🔹 SUBMIT
        binding.submitButton.setOnClickListener {
            val description = binding.descriptionInput.text.toString().trim()
            viewModel.submitBug(description, selectedImageUri?.toString())
        }

        // 🔹 LOADING STATE
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.submitButton.isEnabled = !isLoading
        }

        // 🔹 RESULT HANDLING
        viewModel.submitResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                lifecycleScope.launch {
                    PopupBus.showSuccess("Bug report submitted")
                }
                clearForm()
                findNavController().navigateUp()
            }

            result.onFailure { throwable ->
                lifecycleScope.launch {
                    PopupBus.showError(throwable.message ?: "Bug report failed")
                }
            }
        }
    }

    private fun clearForm() {
        // Clear description input
        binding.descriptionInput.setText("")
        
        // Reset selected image URI
        selectedImageUri = null
        
        // Reset image icon to default
        binding.uploadIcon.setImageResource(defaultUploadIconRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

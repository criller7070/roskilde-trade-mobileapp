package dk.rosswap.mobile.feature.bugreport.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dk.rosswap.mobile.databinding.FragmentBugReportBinding

class ReportBugFragment : Fragment() {

    private var _binding: FragmentBugReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View { // Inflate fragment_bug_report.xml using ViewBinding
        _binding = FragmentBugReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: add TextWatcher for binding.descriptionInput to update binding.charCounter
        // TODO: set up image picker on binding.imageUploadContainer and update binding.uploadIcon
        // TODO: observe viewmodel/state to enable/disable binding.submitButton
        // TODO: handle binding.submitButton click to submit the report

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null         // Clear binding reference to avoid memory leaks
    }
}

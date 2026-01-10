package dk.rosswap.mobile.feature.legal.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dk.rosswap.mobile.databinding.FragmentPrivacyBinding

class PrivacyFragment : Fragment() {

    private var _binding: FragmentPrivacyBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PrivacyViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPrivacyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: observe viewModel to populate dynamic fields (e.g., last updated date)
        // TODO: enable link handling for TextViews if needed (use LinkMovementMethod)
        // TODO: add any click listeners for buttons/links in the privacy layout
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

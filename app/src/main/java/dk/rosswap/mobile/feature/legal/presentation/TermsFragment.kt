package dk.rosswap.mobile.feature.legal.presentation

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.FragmentTermsBinding

class TermsFragment : Fragment(R.layout.fragment_terms) {

    private var _binding: FragmentTermsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentTermsBinding.bind(view)

        setupPersonalDataLink()
    }

    private fun setupPersonalDataLink() {
        val fullText =
            getString(R.string.terms_personal_data_text) +
                    getString(R.string.terms_privacy_policy_link) +
                    getString(R.string.terms_personal_data_suffix)

        val spannable = SpannableString(fullText)

        val linkText = getString(R.string.terms_privacy_policy_link)
        val start = fullText.indexOf(linkText)
        val end = start + linkText.length

        spannable.setSpan(object : ClickableSpan() {

            override fun onClick(widget: View) {
                findNavController().navigate(
                    R.id.action_termsFragment_to_privacyPolicyFragment
                )
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = requireContext().getColor(R.color.brand_orange)
                ds.isUnderlineText = false
            }

        }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        binding.personalDataText.text = spannable
        binding.personalDataText.movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

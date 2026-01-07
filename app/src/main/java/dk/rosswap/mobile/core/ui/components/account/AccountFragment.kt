package dk.rosswap.mobile.core.ui.components.account

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.AccountBinding

@AndroidEntryPoint
class AccountFragment : Fragment(R.layout.account) {

    private var _binding: AccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountViewModel by viewModels()

    private var isTermsExpanded = false   // 🔹 added

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = AccountBinding.bind(view)

        // 🔹 TERMS EXPAND / COLLAPSE LOGIC
        binding.tvReadMore.setOnClickListener {
            if (isTermsExpanded) {
                binding.tvTerms.maxLines = 2
                binding.tvReadMore.text = "Read more"
            } else {
                binding.tvTerms.maxLines = Integer.MAX_VALUE
                binding.tvReadMore.text = "Show less"
            }
            isTermsExpanded = !isTermsExpanded
        }

        binding.btnCreateAccount.setOnClickListener {
            createAccount()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnCreateAccount.isEnabled = !isLoading
        }

        viewModel.createResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                showSuccessDialog()
            }
            result.onFailure { err ->
                Toast.makeText(
                    requireContext(),
                    err.message ?: "Signup failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun createAccount() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val acceptedTerms = binding.cbTerms.isChecked

        viewModel.createAccount(name, email, password, acceptedTerms)
    }

    private fun showSuccessDialog() {
        AccountSuccessDialog {
            findNavController().navigate(R.id.nav_home)
        }.show(parentFragmentManager, "AccountSuccessDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

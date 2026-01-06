package dk.rosswap.mobile.core.ui.components.account

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dk.rosswap.mobile.R
import dk.rosswap.mobile.databinding.AccountBinding

class AccountFragment : Fragment(R.layout.account) {

    private var _binding: AccountBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AccountViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = AccountBinding.bind(view)

        binding.btnCreateAccount.setOnClickListener {
            createAccount()
        }
    }

    private fun createAccount() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val acceptedTerms = binding.cbTerms.isChecked

        // Call ViewModel (logic/validation happens there)
        viewModel.createAccount(
            name = name,
            email = email,
            password = password,
            acceptedTerms = acceptedTerms
        )

        // TEMP: Assume success for now and show success dialog
        showSuccessDialog()
    }

    private fun showSuccessDialog() {
        AccountSuccessDialog {
            // Navigate after user presses OK
            findNavController().navigate(R.id.nav_home)
        }.show(parentFragmentManager, "AccountSuccessDialog")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

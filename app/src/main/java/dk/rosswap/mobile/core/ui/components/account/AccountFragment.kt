package dk.rosswap.mobile.core.ui.components.account

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.databinding.AccountBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
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

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.btnCreateAccount.isEnabled = !isLoading
        }

        viewModel.createResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                // emit a popup event centrally
                viewLifecycleOwner.lifecycleScope.launch {
                    PopupBus.showSuccess("Your account was created.")
                }
                // navigate after emitting (original dialog navigated on dismiss; this navigates immediately)
                findNavController().navigate(R.id.nav_home)
            }
            result.onFailure { err ->
                viewLifecycleOwner.lifecycleScope.launch {
                    PopupBus.showError(err.message ?: "Signup failed")
                }
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

//    private fun showSuccessDialog() {
//        AccountSuccessDialog {
//            findNavController().navigate(R.id.nav_home)
//        }.show(parentFragmentManager, "AccountSuccessDialog")
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

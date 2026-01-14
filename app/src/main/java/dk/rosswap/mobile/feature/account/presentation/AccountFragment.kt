package dk.rosswap.mobile.feature.account.presentation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.databinding.FragmentAccountBinding
import dk.rosswap.mobile.feature.auth.presentation.AuthViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AccountFragment : Fragment(R.layout.fragment_account) {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by activityViewModels()

    private var isTermsExpanded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAccountBinding.bind(view)

        // TERMS EXPAND / COLLAPSE LOGIC
        binding.tvReadMore.setOnClickListener {
            if (isTermsExpanded) {
                binding.tvTerms.maxLines = 2
                binding.tvReadMore.text = getString(R.string.read_more)
            } else {
                binding.tvTerms.maxLines = Integer.MAX_VALUE
                binding.tvReadMore.text = getString(R.string.show_less)
            }
            isTermsExpanded = !isTermsExpanded
        }

        binding.btnCreateAccount.setOnClickListener {
            createAccount()
        }

        // Observe loading and creation states from AuthViewModel
        authViewModel.authStateLiveData.observe(viewLifecycleOwner) { state ->
            binding.btnCreateAccount.isEnabled = state !is dk.rosswap.mobile.core.common.AuthState.Loading
        }
    }

    private fun createAccount() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val acceptedTerms = binding.cbTerms.isChecked

        authViewModel.signUp(email, password, name, acceptedTerms)

        // Observe state and navigate on success
        authViewModel.authStateLiveData.observe(viewLifecycleOwner) { state ->
            if (state is dk.rosswap.mobile.core.common.AuthState.Authenticated) {
                lifecycleScope.launch {
                    PopupBus.showSuccess("Your account was created.")
                }
                findNavController().navigate(R.id.nav_home)
            } else if (state is dk.rosswap.mobile.core.common.AuthState.Error) {
                lifecycleScope.launch {
                    PopupBus.showError(state.exception.message ?: "Signup failed")
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

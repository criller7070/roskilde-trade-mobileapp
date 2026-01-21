package dk.rosswap.mobile.feature.account.presentation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.databinding.FragmentAccountBinding
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AccountFragment : Fragment(R.layout.fragment_account) {
    // binding vars
    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var authRepository: AuthRepository

    private var isTermsExpanded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAccountBinding.bind(view)

        // terms expand/collapse logic
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

        // create Account button
        binding.btnCreateAccount.setOnClickListener {
            createAccount()
        }

        // observe loading and creation states from SessionManager
        sessionManager.authState.asLiveData().observe(viewLifecycleOwner) { state ->
            binding.btnCreateAccount.isEnabled = state !is dk.rosswap.mobile.core.common.AuthState.Loading

            // Handle authentication state changes
            if (state is dk.rosswap.mobile.core.common.AuthState.Authenticated) {
                lifecycleScope.launch {
                    PopupBus.showSuccess("Your account was created.")
                }
                findNavController().navigate(R.id.nav_home)
            } else if (state is dk.rosswap.mobile.core.common.AuthState.Error) {
                lifecycleScope.launch {
                    PopupBus.showError(state.exception.message ?: "Signup failed, are you already logged in?")
                }
            }
        }
    }

    // if createAccount button clicked, call AuthRepository to create account
    private fun createAccount() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()
        val acceptedTerms = binding.cbTerms.isChecked

        lifecycleScope.launch {
            try {
                authRepository.signUp(email, password, name, acceptedTerms)
            } catch (e: Exception) {
                PopupBus.showError(e.message ?: "Signup failed")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

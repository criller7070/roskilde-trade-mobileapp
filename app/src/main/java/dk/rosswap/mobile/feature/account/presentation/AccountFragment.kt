package dk.rosswap.mobile.feature.account.presentation

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.databinding.FragmentAccountBinding
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.launch
import android.util.Log
import android.content.Intent
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

    private lateinit var googleSignInClient: GoogleSignInClient

    private var isTermsExpanded = false

    // helper to enable/disable and visually dim the primary action buttons
    private fun updateButtonsEnabled(hasConsent: Boolean, isLoading: Boolean = false) {
        val enabled = hasConsent && !isLoading
        binding.btnCreateAccount.isEnabled = enabled
        binding.buttonGoogle.isEnabled = enabled
        binding.btnCreateAccount.alpha = if (enabled) 1f else 0.5f
        binding.buttonGoogle.alpha = if (enabled) 1f else 0.5f
    }
    private var pendingGoogleHasConsent: Boolean = false

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // 1. check if result is OK
        if (result.resultCode != Activity.RESULT_OK) {
            lifecycleScope.launch { PopupBus.showError("Google sign-in cancelled or failed.") }
            Log.w("AccountFragment", "Google sign-in cancelled or returned non-OK result: ${result.resultCode}")
            return@registerForActivityResult
        }

        // 2. get data from result
        val data: Intent? = result.data
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken == null) { // fallback
                val msg = "No ID token from Google account. Make sure `requestIdToken(...)` used the correct client ID."
                lifecycleScope.launch { PopupBus.showError(msg) }
                Log.w("AccountFragment", msg)
                return@registerForActivityResult
            }

            // 3. pass consent checkbox to the repository
            lifecycleScope.launch {
                try {
                    // prefer the captured consent from when we launched; fall back to checkbox current state
                    val consent = pendingGoogleHasConsent || binding.cbTerms.isChecked
                    // reset pending flag
                    pendingGoogleHasConsent = false
                    val result = authRepository.signInWithGoogle(idToken, hasConsent = consent)
                    if (result.isSuccess) {
                        PopupBus.showSuccess("Sign-up successful.")
                        findNavController().navigate(R.id.nav_home)
                    } else {
                        PopupBus.showError(result.exceptionOrNull()?.message ?: "Google sign-in failed")
                    }
                } catch (e: Exception) {
                    PopupBus.showError(e.message ?: "Google sign-in failed")
                }
            }

        } catch (e: ApiException) {
            val status = e.statusCode
            val message = "Google sign-in failed (status=$status): ${e.message}"
            lifecycleScope.launch { PopupBus.showError(message) }
            Log.e("AccountFragment", "Google sign-in ApiException (status=$status)", e)
        }
    }

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

        // configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        binding.buttonGoogle.setOnClickListener {
            // capture the consent value now (prevents race where user toggles while Google activity is shown)
            pendingGoogleHasConsent = binding.cbTerms.isChecked
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }

        // keep buttons disabled/grayed until user accepts terms
        updateButtonsEnabled(binding.cbTerms.isChecked, isLoading = false)
        binding.cbTerms.setOnCheckedChangeListener { _, isChecked ->
            updateButtonsEnabled(isChecked, isLoading = false)
        }

        // observe loading and creation states from SessionManager
        sessionManager.authState.asLiveData().observe(viewLifecycleOwner) { state ->
            val isLoading = state is dk.rosswap.mobile.core.common.AuthState.Loading
            // Respect both terms consent and loading state when enabling buttons
            updateButtonsEnabled(binding.cbTerms.isChecked, isLoading)

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

package dk.rosswap.mobile.feature.auth.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.feature.auth.domain.GoogleSignInUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    @Inject
    lateinit var googleSignInUseCase: GoogleSignInUseCase

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            // User cancelled or sign-in failed early
            lifecycleScope.launch { PopupBus.showError("Google sign-in cancelled or failed.") }
            return@registerForActivityResult
        }

        val data: Intent? = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken == null) {
                lifecycleScope.launch { PopupBus.showError("No ID token from Google account.") }
                return@registerForActivityResult
            }

            // Use ViewModel's signInWithGoogle
            authViewModel.signInWithGoogle(idToken)
        } catch (e: ApiException) {
            lifecycleScope.launch { PopupBus.showError("Google sign-in failed: ${e.message}") }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = view.findViewById<EditText>(R.id.edit_email)
        val password = view.findViewById<EditText>(R.id.edit_password)
        val loginBtn = view.findViewById<Button>(R.id.button_login)
        val googleBtn = view.findViewById<Button>(R.id.button_google)
        val createAccount = view.findViewById<TextView>(R.id.text_create_account)

        // Obtain GoogleSignInClient from use case (centralized configuration)
        googleSignInClient = googleSignInUseCase.getGoogleSignInClient()

        loginBtn.setOnClickListener {
            val e = email.text.toString().trim()
            val p = password.text.toString().trim()
            authViewModel.login(e, p)
        }

        googleBtn.setOnClickListener {
            // Start the Google sign-in flow
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }

        createAccount.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                findNavController().navigate(R.id.action_nav_login_to_createAccount)
            }
        }

        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loginBtn.isEnabled = !isLoading
        }

        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                viewLifecycleOwner.lifecycleScope.launch {
                    PopupBus.showSuccess("Login successful.")
                }
                // Navigate to Home and remove Login from back stack
                findNavController().navigate(
                    R.id.action_nav_login_to_home,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_login_required, inclusive = true)
                        .build()
                )
            }
            result.onFailure { throwable ->
                viewLifecycleOwner.lifecycleScope.launch {
                    PopupBus.showError(throwable.message ?: "Login failed")
                }
            }
        }
    }
}

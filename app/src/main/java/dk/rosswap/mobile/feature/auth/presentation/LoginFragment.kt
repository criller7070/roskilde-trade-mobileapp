@file:Suppress("DEPRECATION")

package dk.rosswap.mobile.feature.auth.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.asLiveData
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.feature.auth.domain.AuthRepository
import dk.rosswap.mobile.core.common.SessionManager
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "LoginFragment"

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var authRepository: AuthRepository

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // A User cancelled or sign-in failed early
        if (result.resultCode != Activity.RESULT_OK) {
            lifecycleScope.launch { PopupBus.showError("Google sign-in cancelled or failed.") }
            Log.w(TAG, "Google sign-in cancelled or returned non-OK result: ${result.resultCode}")
            return@registerForActivityResult
        }

        // B User signed in successfully, get ID token and sign in with Firebase
        val data: Intent? = result.data
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken == null) {
                val msg = "No ID token from Google account. Make sure `requestIdToken(...)` used the correct client ID."
                lifecycleScope.launch { PopupBus.showError(msg) }
                Log.w(TAG, msg)
                return@registerForActivityResult
            }

            viewModel.signInWithGoogle(idToken)

        } catch (e: ApiException) {
            val status = e.statusCode
            val message = "Google sign-in failed (status=$status): ${e.message}"
            lifecycleScope.launch { PopupBus.showError(message) }
            Log.e(TAG, "Google sign-in ApiException (status=$status)", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // go back to home if already logged in
        val email = view.findViewById<EditText>(R.id.edit_email)
        val password = view.findViewById<EditText>(R.id.edit_password)
        val loginBtn = view.findViewById<Button>(R.id.button_login)
        val googleBtn = view.findViewById<Button>(R.id.button_google)
        val createAccount = view.findViewById<TextView>(R.id.text_create_account)

        // configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        loginBtn.setOnClickListener {
            val e = email.text.toString().trim()
            val p = password.text.toString().trim()
            viewModel.login(e, p)
        }

        googleBtn.setOnClickListener {
            // Start the Google sign-in flow
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }

        createAccount.apply { // navigate to create account
            isClickable = true
            isFocusable = true
            setOnClickListener {
                findNavController().navigate(R.id.action_nav_login_to_createAccount)
            }
        }

        // observe ViewModel ui state and events
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                loginBtn.isEnabled = !state.isLoading
                // optionally show progress UI here
            }
        }

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is LoginEvent.NavigateToHome -> {
                        // Show a success message; navigation is centralized in MainActivity.
                        PopupBus.showSuccess("Login successful.")
                    }
                    is LoginEvent.ShowError -> {
                        PopupBus.showError(event.message)
                    }
                }
            }
        }

        // still observe SessionManager.authState for global changes/errors
        sessionManager.authState.asLiveData().observe(viewLifecycleOwner) { state ->
            // if authed
            if (state is dk.rosswap.mobile.core.common.AuthState.Authenticated) {
                // show a brief success message; navigation to home is handled centrally.
                lifecycleScope.launch { PopupBus.showSuccess("Login successful.") }
            } else if (state is dk.rosswap.mobile.core.common.AuthState.Error) {
                lifecycleScope.launch { PopupBus.showError(state.exception.message ?: "Login failed") }
            }
        }
    }
}

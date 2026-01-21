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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import kotlinx.coroutines.launch

private const val TAG = "LoginFragment"

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val authViewModel: AuthViewModel by activityViewModels()

    private lateinit var googleSignInClient: GoogleSignInClient

    private val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        // 1A User cancelled or sign-in failed early
        if (result.resultCode != Activity.RESULT_OK) {
            lifecycleScope.launch { PopupBus.showError("Google sign-in cancelled or failed.") }
            Log.w(TAG, "Google sign-in cancelled or returned non-OK result: ${result.resultCode}")
            return@registerForActivityResult
        }

        // 1B User signed in successfully, get ID token and sign in with Firebase
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

            authViewModel.signInWithGoogle(idToken)

        } catch (e: ApiException) {
            val status = e.statusCode
            val message = "Google sign-in failed (status=$status): ${e.message}"
            lifecycleScope.launch { PopupBus.showError(message) }
            Log.e(TAG, "Google sign-in ApiException (status=$status)", e)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = view.findViewById<EditText>(R.id.edit_email)
        val password = view.findViewById<EditText>(R.id.edit_password)
        val loginBtn = view.findViewById<Button>(R.id.button_login)
        val googleBtn = view.findViewById<Button>(R.id.button_google)
        val createAccount = view.findViewById<TextView>(R.id.text_create_account)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

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

        authViewModel.isLoading.observe(viewLifecycleOwner) { isLoading: Boolean ->
            loginBtn.isEnabled = !isLoading
        }

        authViewModel.loginResult.observe(viewLifecycleOwner) { result: Result<Unit> ->
            // login result
            if (result.isSuccess) {
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
            } else {
                val ex = result.exceptionOrNull()
                viewLifecycleOwner.lifecycleScope.launch {
                    PopupBus.showError(ex?.message ?: "Login failed")
                }
            }
        }

        // Observe auth state and navigate when authenticated
        authViewModel.authStateLiveData.observe(viewLifecycleOwner) { state ->
            if (state is dk.rosswap.mobile.core.common.AuthState.Authenticated) {
                viewLifecycleOwner.lifecycleScope.launch { PopupBus.showSuccess("Login successful.") }
                findNavController().navigate(
                    R.id.action_nav_login_to_home,
                    null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_login_required, inclusive = true)
                        .build()
                )
            } else if (state is dk.rosswap.mobile.core.common.AuthState.Error) {
                viewLifecycleOwner.lifecycleScope.launch { PopupBus.showError(state.exception.message ?: "Login failed") }
            }
        }
    }
}

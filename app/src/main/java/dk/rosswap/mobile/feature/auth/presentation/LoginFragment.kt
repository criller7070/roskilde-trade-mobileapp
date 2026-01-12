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
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private lateinit var googleSignInClient: GoogleSignInClient

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

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener { authResult ->
                    if (authResult.isSuccessful) {
                        // Show success and navigate (same behaviour as email login)
                        lifecycleScope.launch { PopupBus.showSuccess("Login successful.") }
                        findNavController().navigate(
                            R.id.action_nav_login_to_home,
                            null,
                            androidx.navigation.NavOptions.Builder()
                                .setPopUpTo(R.id.nav_login_required, true)
                                .build()
                        )
                    } else {
                        val msg = authResult.exception?.message ?: "Authentication failed"
                        lifecycleScope.launch { PopupBus.showError(msg) }
                    }
                }
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

        // Configure Google Sign-In
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

        createAccount.apply {
            isClickable = true
            isFocusable = true
            setOnClickListener {
                findNavController().navigate(R.id.action_nav_login_to_createAccount)
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loginBtn.isEnabled = !isLoading
        }

        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
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

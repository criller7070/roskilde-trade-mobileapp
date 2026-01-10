package dk.rosswap.mobile.feature.auth.presentation

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = view.findViewById<EditText>(R.id.edit_email)
        val password = view.findViewById<EditText>(R.id.edit_password)
        val loginBtn = view.findViewById<Button>(R.id.button_login)
        val googleBtn = view.findViewById<Button>(R.id.button_google)
        val createAccount = view.findViewById<TextView>(R.id.text_create_account)

        loginBtn.setOnClickListener {
            val e = email.text.toString().trim()
            val p = password.text.toString().trim()
            viewModel.login(e, p)
        }

        googleBtn.setOnClickListener {
            // TODO: start Google sign-in flow
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

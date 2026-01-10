package dk.rosswap.mobile.feature.auth.presentation

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    private val viewModel: LoginViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { // after view created...
        super.onViewCreated(view, savedInstanceState) // call super

        val email = view.findViewById<EditText>(R.id.edit_email)
        val password = view.findViewById<EditText>(R.id.edit_password)
        val loginBtn = view.findViewById<Button>(R.id.button_login) // login button
        val googleBtn = view.findViewById<Button>(R.id.button_google) // google sign-in button
        val createAccount = view.findViewById<TextView>(R.id.text_create_account) // register link

        loginBtn.setOnClickListener {
            val e = email.text.toString().trim()
            val p = password.text.toString().trim()
            viewModel.login(e, p)
            // Example navigation (uncomment and replace with the action id from your nav graph):
            // findNavController().navigate(R.id.action_login_to_home)
        }

        googleBtn.setOnClickListener {
            // TODO: start Google sign-in flow
        }

        createAccount.setOnClickListener {
            // Example navigation to register (replace with your action id):
            // findNavController().navigate(R.id.action_login_to_register)
        }

        // enable/disable button while loading
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loginBtn.isEnabled = !isLoading
        }

        // show popup on success / failure
        viewModel.loginResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                viewLifecycleOwner.lifecycleScope.launch {
                    PopupBus.showSuccess("Login successful.")
                }
            }
            result.onFailure { throwable ->
                viewLifecycleOwner.lifecycleScope.launch {
                    PopupBus.showError(throwable?.message ?: "Login failed")
                }
            }
        }
    }
}

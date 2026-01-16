package dk.rosswap.mobile.feature.auth.presentation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.core.ui.observeAuthState

@AndroidEntryPoint
class LoginRequiredFragment : Fragment(R.layout.fragment_login_required) {

    private val TAG = "LoginRequiredFragment"
    private val authViewModel: AuthViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeAuthState(authViewModel.authState) { state ->
            when (state) {
                is AuthState.Authenticated -> {
                    Log.d(TAG, "User authenticated while LoginRequired is visible; closing")
                    // Pop back so the user returns to previous destination (or change to desired nav)
                    findNavController().popBackStack()
                }
                is AuthState.Loading -> {
                    // Could show a spinner if the layout supports it
                }
                is AuthState.Unauthenticated -> {
                    // Keep showing login/create buttons
                }
                is AuthState.Error -> {
                    Log.w(TAG, "Auth error while on LoginRequired: ${state.exception.message}")
                }
            }
        }

        val loginBtn = view.findViewById<MaterialButton>(R.id.btn_login)
        val createBtn = view.findViewById<MaterialButton>(R.id.btn_create_account)

        loginBtn.setOnClickListener {
            Log.i(TAG, "Navigate to login requested")
            findNavController().navigate(R.id.action_nav_login_required_to_login)
        }

        createBtn.setOnClickListener {
            Log.i(TAG, "Navigate to create account requested")
            findNavController().navigate(R.id.action_nav_login_required_to_createAccount)
        }
    }
}

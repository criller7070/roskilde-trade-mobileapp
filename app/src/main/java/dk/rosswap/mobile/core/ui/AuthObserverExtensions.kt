package dk.rosswap.mobile.core.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.feature.auth.presentation.AuthViewModel
import kotlinx.coroutines.launch

fun Fragment.observeAuthState(
    authViewModel: AuthViewModel,
    action: (AuthState) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            authViewModel.authState.collect { state ->
                action(state)
            }
        }
    }
}

package dk.rosswap.mobile.core.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dk.rosswap.mobile.core.common.AuthState
import dk.rosswap.mobile.feature.auth.presentation.LoginRequiredViewModel
import kotlinx.coroutines.launch

/**
 * Extension function to observe AuthState in a Fragment.
 * Handles the lifecycle-aware observation of authentication state.
 *
 * Usage:
 * ```kotlin
 * observeAuthState(authViewModel) { state ->
 *     when (state) {
 *         is AuthState.Loading -> showLoadingScreen()
 *         is AuthState.Authenticated -> showMainApp(state.user)
 *         is AuthState.Unauthenticated -> showLoginScreen()
 *         is AuthState.Error -> showError(state.exception)
 *     }
 * }
 * ```
 */
fun Fragment.observeAuthState(
    loginRequiredViewModel: LoginRequiredViewModel,
    action: (AuthState) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            loginRequiredViewModel.authState.collect { state ->
                action(state)
            }
        }
    }
}

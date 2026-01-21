package dk.rosswap.mobile.core.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dk.rosswap.mobile.core.common.AuthState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// This file is a bit of a bother. it exists because leaks can occur if coroutines (async) haven't
// started their lifecycles properly. This observer listens for that and only activates the entire
// auth state process ("flow") when the view is started.

fun Fragment.observeAuthState(
    authStateFlow: StateFlow<AuthState>,
    action: (AuthState) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            authStateFlow.collect { state ->
                action(state)
            }
        }
    }
}

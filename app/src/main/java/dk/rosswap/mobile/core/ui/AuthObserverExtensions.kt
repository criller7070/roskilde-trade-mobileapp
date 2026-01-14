package dk.rosswap.mobile.core.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dk.rosswap.mobile.core.common.AuthStateImpl
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

fun Fragment.observeAuthState(
    authStateImplFlow: StateFlow<AuthStateImpl>,
    action: (AuthStateImpl) -> Unit
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            authStateImplFlow.collect { state ->
                action(state)
            }
        }
    }
}

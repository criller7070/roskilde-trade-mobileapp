package dk.rosswap.mobile.core.common

import kotlinx.coroutines.flow.StateFlow

interface AuthState {
    interface Provider {
        val authState: StateFlow<AuthState>
    }
}

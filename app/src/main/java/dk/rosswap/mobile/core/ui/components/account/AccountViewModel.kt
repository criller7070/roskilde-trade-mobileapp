package dk.rosswap.mobile.core.ui.components.account

import androidx.lifecycle.ViewModel

class AccountViewModel : ViewModel() {

    fun createAccount(
        name: String,
        email: String,
        password: String,
        acceptedTerms: Boolean
    ) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return
        }

        if (!acceptedTerms) {
            return
        }

        // TODO: implement logic
    }
}

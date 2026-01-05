package dk.rosswap.mobile.core.ui.components.login

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R

@AndroidEntryPoint
class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = view.findViewById<EditText>(R.id.edit_email)
        val password = view.findViewById<EditText>(R.id.edit_password)
        val loginBtn = view.findViewById<Button>(R.id.button_login)
        val googleBtn = view.findViewById<Button>(R.id.button_google)
        val createAccount = view.findViewById<TextView>(R.id.text_create_account)

        loginBtn.setOnClickListener {
            // TODO: validate credentials and perform auth
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
    }
}

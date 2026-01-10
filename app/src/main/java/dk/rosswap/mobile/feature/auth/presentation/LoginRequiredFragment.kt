package dk.rosswap.mobile.feature.auth.presentation

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import dagger.hilt.android.AndroidEntryPoint
import dk.rosswap.mobile.R
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginRequiredFragment : Fragment(R.layout.fragment_login_required) {

    private val TAG = "LoginRequiredFragment"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = view.findViewById<TextView>(R.id.tv_not_logged_title)
        val subtitle = view.findViewById<TextView>(R.id.tv_not_logged_sub)
        val loginBtn = view.findViewById<MaterialButton>(R.id.btn_login)
        val createBtn = view.findViewById<MaterialButton>(R.id.btn_create_account)

        loginBtn.setOnClickListener {
            Log.i(TAG, "Navigate to login requested")
            // replace with your actual navigation id if available:
            // findNavController().navigate(R.id.loginFragment)
            // fallback: show a popup so the press has visible feedback
            viewLifecycleOwner.lifecycleScope.launch {
                PopupBus.showSuccess("Navigating to login (replace nav id with actual destination)")
            }
        }

        createBtn.setOnClickListener {
            Log.i(TAG, "Navigate to create account requested")
            // replace with your actual navigation id if available:
            // findNavController().navigate(R.id.createAccountFragment)
            viewLifecycleOwner.lifecycleScope.launch {
                PopupBus.showSuccess("Navigating to create account (replace nav id with actual destination)")
            }
        }
    }
}

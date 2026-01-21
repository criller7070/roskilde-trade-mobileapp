package dk.rosswap.mobile.feature.auth.presentation

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dk.rosswap.mobile.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LogoutDialogFragment : DialogFragment() {

    private val viewModel: LogoutViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.dialog_logout, container, false)

        val btnCancel = view.findViewById<View>(R.id.btnCancel)
        val btnConfirm = view.findViewById<View>(R.id.btnConfirm)

        btnCancel.setOnClickListener { dismiss() }
        btnConfirm.setOnClickListener {
            viewModel.signOut()
        }

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // observe sign out events
        lifecycleScope.launch {
            viewModel.events.collect { event ->
                when (event) {
                    is LogoutEvent.SignedOut -> {
                        cleanupAndGoToLogin()
                    }
                    is LogoutEvent.Error -> {
                        cleanupAndGoToLogin() // still cleanup; show error
                        Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        return view
    }

    private fun cleanupAndGoToLogin() {

        // Restart the app's MainActivity which will choose the correct start destination
        val mainActivityName = "dk.rosswap.mobile.MainActivity"

        val intent = Intent().setClassName(requireContext(), mainActivityName).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)

        Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
        dismiss()
    }
}
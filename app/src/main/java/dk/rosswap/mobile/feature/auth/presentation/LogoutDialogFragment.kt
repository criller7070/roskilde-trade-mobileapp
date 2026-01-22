package dk.rosswap.mobile.feature.auth.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dk.rosswap.mobile.R
import androidx.core.content.edit

class LogoutDialogFragment : DialogFragment() {

    private val authViewModel: AuthViewModel by activityViewModels()

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
            performSignOut()
        }

        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return view
    }

    private fun performSignOut() {
        // Delegate sign-out to ViewModel instead of handling Firebase directly
        authViewModel.signOut()
        cleanupAndGoToLogin()
    }

    private fun cleanupAndGoToLogin() {
        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit {
                clear()
            }

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
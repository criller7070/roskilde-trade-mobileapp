package dk.rosswap.mobile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import dk.rosswap.mobile.R

class LogoutDialogFragment : DialogFragment() {

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
        FirebaseAuth.getInstance().signOut()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(requireContext(), gso)

        googleClient.signOut()
            .addOnCompleteListener {
                cleanupAndGoToLogin()
            }
            .addOnFailureListener {
                cleanupAndGoToLogin()
            }
    }

    private fun cleanupAndGoToLogin() {
        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()

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
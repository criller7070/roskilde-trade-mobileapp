package dk.rosswap.mobile

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LogoutDialogFragment : DialogFragment() {

    @Inject lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View = layoutInflater.inflate(R.layout.dialog_logout, null)

        val builder = MaterialAlertDialogBuilder(requireContext())
            .setView(view)

        val dialog = builder.create()

        // Hook buttons from the inflated layout
        val btnCancel = view.findViewById<View>(R.id.btnCancel)
        val btnConfirm = view.findViewById<View>(R.id.btnConfirm)
        val card = view.findViewById<View>(R.id.logout_card)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            // Sign out and restart MainActivity so nav graph picks login-required
            firebaseAuth.signOut()
            dialog.dismiss()
            val activity = requireActivity()
            val currentIntent = activity.intent
            activity.finish()
            activity.startActivity(currentIntent)
        }

        dialog.setCanceledOnTouchOutside(true)
        dialog.setOnShowListener { _ ->
            // Ensure dialog window size and positioning are wrapped and centered
            val window = dialog.window
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val params = window?.attributes
            params?.gravity = Gravity.CENTER
            params?.dimAmount = 0.45f

            // Compute visible display frame (excludes status bar) and adjust window Y offset
            try {
                val visible = Rect()
                window?.decorView?.getWindowVisibleDisplayFrame(visible)
                val visibleCenterY = visible.top + visible.height() / 2

                // measure card on screen
                val loc = IntArray(2)
                card.getLocationOnScreen(loc)
                val cardCenterY = loc[1] + card.height / 2

                val delta = visibleCenterY - cardCenterY
                // window.y moves the window relative to gravity, so set it to delta
                params?.y = delta
            } catch (_: Exception) {
                // ignore failures
            }

            window?.attributes = params as WindowManager.LayoutParams
        }

        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        // no-op
    }
}

package dk.rosswap.mobile

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton

class LogoutDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // translucent, no title bar so window can be full screen and show dim behind
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // inflate using the provided container so layout params on the root are resolved
        val view = inflater.inflate(R.layout.dialog_logout, container, false)

        view.findViewById<MaterialButton>(R.id.btnCancel)?.setOnClickListener {
            dismiss()
        }
        view.findViewById<MaterialButton>(R.id.btnConfirm)?.setOnClickListener {
            // perform sign out, then dismiss
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            // use KTX toDrawable to satisfy linter suggestion
            window.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            window.decorView.setPadding(0, 0, 0, 0)
            val params = window.attributes
            params.gravity = Gravity.CENTER
            params.dimAmount = 0.45f
            window.attributes = params

            // Use native window background blur on Android 12+ for a nicer backdrop effect.
            // Fallback: older devices will use the dimAmount above.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    // Small radius; tweak as needed (0 - 100+). A larger value increases blurriness.
                    window.setBackgroundBlurRadius(40)
                } catch (_: Throwable) {
                    // No-op: if anything goes wrong, keep the dim fallback. Don't crash.
                }
            }
        }
    }
}
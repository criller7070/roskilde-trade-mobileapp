package dk.rosswap.mobile

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
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
        // inflate with null parent so layout params inside the layout are respected
        val view = inflater.inflate(R.layout.dialog_logout, null, false)

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
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.decorView.setPadding(0, 0, 0, 0)
            val params = window.attributes
            params.gravity = Gravity.CENTER
            params.dimAmount = 0.45f
            window.attributes = params
        }
    }
}
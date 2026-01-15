package dk.rosswap.mobile.core.ui.components.popup

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.snackbar.Snackbar
import dk.rosswap.mobile.R

class PopupHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.component_popup_host, this, true)
    }

    fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(this, message, duration).show()
    }

    fun dismiss() {
        // Hide the host itself
        this.visibility = GONE
    }

    companion object {
        fun install(activity: Activity) {
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            // Avoid installing multiple hosts
            val existing = root.findViewById<PopupHost?>(R.id.popup_host_root)
            if (existing != null) return
            val host = PopupHost(activity)
            root.addView(host)
        }
    }
}

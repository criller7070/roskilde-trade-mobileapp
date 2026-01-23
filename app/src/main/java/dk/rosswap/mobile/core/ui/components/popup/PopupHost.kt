package dk.rosswap.mobile.core.ui.components.popup

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import dk.rosswap.mobile.R
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner

// Difference between PopupBus and Host is that PopupBus is the event source (the observable bus),
// while PopupHost is the UI observer that listens to those events and displays dialogs.

class PopupHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var currentDialog: AlertDialog? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.component_popup_host, this, true)

        val lifecycleOwner = (context as? LifecycleOwner)
        lifecycleOwner?.let { owner ->
            PopupBus.events.observe(owner) { event ->
                // if event is null, dismiss the dialog
                if (event == null) {
                    currentDialog?.dismiss()
                    currentDialog = null
                    this.visibility = GONE
                } else { // otherwise, show the dialog
                    // Show the host
                    this.visibility = VISIBLE

                    val title = when (event.type) {
                        PopupType.ERROR -> "Error"
                        PopupType.WARNING -> "Warning"
                        PopupType.SUCCESS -> "Success"
                        else -> ""
                    }

                    // Dismiss any existing dialog first
                    currentDialog?.dismiss()

                    // And then create new dialog
                    val builder = AlertDialog.Builder(context)
                        .setTitle(title)
                        .setMessage(event.message)
                        .setPositiveButton("OK") { dlg, _ ->
                            dlg.dismiss()
                            PopupBus.dismiss()
                        }
                        .setOnDismissListener {
                            PopupBus.dismiss()
                        }

                    currentDialog = builder.create()
                    currentDialog?.show()
                }
            }
        }
    }

    companion object {
        fun install(activity: Activity) {
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            val existing = root.findViewById<PopupHost?>(R.id.popup_host_root)
            if (existing != null) return
            val host = PopupHost(activity)
            root.addView(host)
        }
    }
}

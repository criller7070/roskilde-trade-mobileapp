package dk.rosswap.mobile.core.ui.components.popup

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.material.snackbar.Snackbar
import dk.rosswap.mobile.R
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner

class PopupHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var currentDialog: AlertDialog? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.component_popup_host, this, true)

        // Observe global popup events if we are attached to a lifecycle owner
        // The activity will call PopupHost.install which will add this view to the
        // activity content view. If the context is an Activity and it implements
        // LifecycleOwner, observe the static events LiveData.
        val lifecycleOwner = (context as? LifecycleOwner)
        lifecycleOwner?.let { owner ->
            PopupBus.events.observe(owner) { event ->
                if (event == null) {
                    currentDialog?.dismiss()
                    currentDialog = null
                    this.visibility = GONE
                } else {
                    // Make host visible when showing dialogs
                    this.visibility = VISIBLE

                    // Build a simple alert dialog that matches the screenshot style
                    val title = when (event.type) {
                        PopupType.ERROR -> "Error"
                        PopupType.WARNING -> "Warning"
                        PopupType.SUCCESS -> "Success"
                        else -> ""
                    }

                    // Dismiss any existing dialog first
                    currentDialog?.dismiss()

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
            val existing = root.findViewById<PopupHost?>(R.id.popup_host_root)
            if (existing != null) return
            val host = PopupHost(activity)
            root.addView(host)
        }
    }
}

package dk.rosswap.mobile.core.ui.components.popup

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.google.android.material.snackbar.Snackbar
import dk.rosswap.mobile.databinding.ComponentPopupHostBinding

/**
 * PopupHost is a custom view that displays popup messages.
 * Typically used as a container in activities to show global notifications.
 */
class PopupHost @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ComponentPopupHostBinding.inflate(LayoutInflater.from(context), this)

    fun showSnackbar(message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        Snackbar.make(this, message, duration).show()
    }

    fun dismiss() {
        binding.root.visibility = GONE
    }
}

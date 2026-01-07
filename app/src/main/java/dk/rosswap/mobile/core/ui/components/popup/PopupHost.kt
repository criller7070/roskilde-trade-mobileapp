package dk.rosswap.mobile.core.ui.components.popup

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object PopupHost {
    fun install(activity: AppCompatActivity) {
        activity.lifecycleScope.launch {
            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect popup events and show simple informational dialogs
                PopupBus.events.collectLatest { evt ->
                    when (evt) {
                        is PopupEvent.Success -> {
                            AlertDialog.Builder(activity)
                                .setTitle(evt.title)
                                .setMessage(evt.message)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                        is PopupEvent.Error -> {
                            AlertDialog.Builder(activity)
                                .setTitle(evt.title)
                                .setMessage(evt.message)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                        is PopupEvent.Info -> {
                            AlertDialog.Builder(activity)
                                .setTitle(evt.title)
                                .setMessage(evt.message)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                        is PopupEvent.Confirm -> {
                            AlertDialog.Builder(activity)
                                .setTitle(evt.title)
                                .setMessage(evt.message)
                                .setPositiveButton(evt.confirmText) { _, _ ->
                                    evt.onConfirm?.invoke()
                                }
                                .setNegativeButton(evt.cancelText) { _, _ ->
                                    evt.onCancel?.invoke()
                                }
                                .show()
                        }
                    }
                }
            }
        }
    }
}

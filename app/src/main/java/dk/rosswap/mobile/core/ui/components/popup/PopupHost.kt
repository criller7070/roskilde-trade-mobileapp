package dk.rosswap.mobile.core.ui.components.popup

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PopupHost {
    fun install(activity: AppCompatActivity) {
        activity.lifecycleScope.launch {
            activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect popup events and show simple informational dialogs
                PopupBus.events.collect { evt ->
                    // Suspend until the dialog is dismissed to ensure sequential display
                    suspendCancellableCoroutine<Unit> { continuation ->
                        val dialog = when (evt) {
                            is PopupEvent.Success -> {
                                AlertDialog.Builder(activity)
                                    .setTitle(evt.title)
                                    .setMessage(evt.message)
                                    .setPositiveButton("OK") { _, _ ->
                                        continuation.resume(Unit)
                                    }
                                    .create()
                            }
                            is PopupEvent.Error -> {
                                AlertDialog.Builder(activity)
                                    .setTitle(evt.title)
                                    .setMessage(evt.message)
                                    .setPositiveButton("OK") { _, _ ->
                                        continuation.resume(Unit)
                                    }
                                    .create()
                            }
                            is PopupEvent.Info -> {
                                AlertDialog.Builder(activity)
                                    .setTitle(evt.title)
                                    .setMessage(evt.message)
                                    .setPositiveButton("OK") { _, _ ->
                                        continuation.resume(Unit)
                                    }
                                    .create()
                            }
                            is PopupEvent.Confirm -> {
                                AlertDialog.Builder(activity)
                                    .setTitle(evt.title)
                                    .setMessage(evt.message)
                                    .setPositiveButton(evt.confirmText) { _, _ ->
                                        evt.onConfirm?.invoke()
                                        continuation.resume(Unit)
                                    }
                                    .setNegativeButton(evt.cancelText) { _, _ ->
                                        evt.onCancel?.invoke()
                                        continuation.resume(Unit)
                                    }
                                    .create()
                            }
                        }
                        
                        dialog.setOnDismissListener {
                            if (continuation.isActive) {
                                continuation.resume(Unit)
                            }
                        }
                        
                        continuation.invokeOnCancellation {
                            dialog.dismiss()
                        }
                        
                        dialog.show()
                    }
                }
            }
        }
    }
}

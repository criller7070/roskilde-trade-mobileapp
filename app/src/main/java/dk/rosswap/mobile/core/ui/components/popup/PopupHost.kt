package dk.rosswap.mobile.core.ui.components.popup

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CancellableContinuation
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
                                createSimpleDialog(activity, evt.title, evt.message, continuation)
                            }
                            is PopupEvent.Error -> {
                                createSimpleDialog(activity, evt.title, evt.message, continuation)
                            }
                            is PopupEvent.Info -> {
                                createSimpleDialog(activity, evt.title, evt.message, continuation)
                            }
                            is PopupEvent.Confirm -> {
                                createConfirmDialog(activity, evt, continuation)
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
    
    private fun createSimpleDialog(
        activity: AppCompatActivity,
        title: String,
        message: String,
        continuation: CancellableContinuation<Unit>
    ): AlertDialog {
        return AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
            .setOnDismissListener {
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
            .create()
    }
    
    private fun createConfirmDialog(
        activity: AppCompatActivity,
        evt: PopupEvent.Confirm,
        continuation: CancellableContinuation<Unit>
    ): AlertDialog {
        return AlertDialog.Builder(activity)
            .setTitle(evt.title)
            .setMessage(evt.message)
            .setPositiveButton(evt.confirmText) { _, _ ->
                evt.onConfirm?.invoke()
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
            .setNegativeButton(evt.cancelText) { _, _ ->
                evt.onCancel?.invoke()
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
            .setOnDismissListener {
                // Resume continuation when dismissed without a button click
                // (e.g., back button or outside tap)
                if (continuation.isActive) {
                    continuation.resume(Unit)
                }
            }
            .create()
    }
}

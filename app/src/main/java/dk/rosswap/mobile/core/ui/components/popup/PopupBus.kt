package dk.rosswap.mobile.core.ui.components.popup

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PopupBus {
    private const val TAG = "PopupBus"
    private val _events = MutableSharedFlow<PopupEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    // Suspend APIs (reliable delivery)
    suspend fun showSuccess(message: String, title: String = "Success") {
        _events.emit(PopupEvent.Success(title = title, message = message))
    }

    suspend fun showError(message: String, title: String = "Error") {
        _events.emit(PopupEvent.Error(title = title, message = message))
    }

    suspend fun showInfo(message: String, title: String = "Info") {
        _events.emit(PopupEvent.Info(title = title, message = message))
    }

    suspend fun showConfirm(
        message: String,
        title: String = "Confirm",
        confirmText: String = "OK",
        cancelText: String = "Cancel",
        onConfirm: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        _events.emit(
            PopupEvent.Confirm(
                title = title,
                message = message,
                confirmText = confirmText,
                cancelText = cancelText,
                onConfirm = onConfirm,
                onCancel = onCancel
            )
        )
    }

    // Non-suspending convenience methods (may drop events if buffer is full)
    // WARNING: These methods use tryEmit which may drop events if the buffer (extraBufferCapacity = 8)
    // is full or there are no active collectors. When an event is dropped, a warning will be logged
    // to Android's system log (using Log.w). Check logcat for warnings with tag "PopupBus".
    // For guaranteed delivery, use the suspending APIs (showSuccess, showError, etc.) instead.

    /**
     * Posts a success popup. May drop the event if buffer is full or no collectors are active.
     * Logs a warning to logcat (tag "PopupBus") if the event is dropped.
     */
    fun postSuccess(message: String, title: String = "Success") {
        val emitted = _events.tryEmit(PopupEvent.Success(title = title, message = message))
        if (!emitted) {
            Log.w(TAG, "Failed to emit Success popup: buffer full or no collectors. Message: $message")
        }
    }

    /**
     * Posts an error popup. May drop the event if buffer is full or no collectors are active.
     * Logs a warning to logcat (tag "PopupBus") if the event is dropped.
     */
    fun postError(message: String, title: String = "Error") {
        val emitted = _events.tryEmit(PopupEvent.Error(title = title, message = message))
        if (!emitted) {
            Log.w(TAG, "Failed to emit Error popup: buffer full or no collectors. Message: $message")
        }
    }

    /**
     * Posts an info popup. May drop the event if buffer is full or no collectors are active.
     * Logs a warning to logcat (tag "PopupBus") if the event is dropped.
     */
    fun postInfo(message: String, title: String = "Info") {
        val emitted = _events.tryEmit(PopupEvent.Info(title = title, message = message))
        if (!emitted) {
            Log.w(TAG, "Failed to emit Info popup: buffer full or no collectors. Message: $message")
        }
    }

    /**
     * Posts a confirm popup. May drop the event if buffer is full or no collectors are active.
     * Logs a warning to logcat (tag "PopupBus") if the event is dropped.
     */
    fun postConfirm(
        message: String,
        title: String = "Confirm",
        confirmText: String = "OK",
        cancelText: String = "Cancel",
        onConfirm: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        val emitted = _events.tryEmit(
            PopupEvent.Confirm(
                title = title,
                message = message,
                confirmText = confirmText,
                cancelText = cancelText,
                onConfirm = onConfirm,
                onCancel = onCancel
            )
        )
        if (!emitted) {
            Log.w(TAG, "Failed to emit Confirm popup: buffer full or no collectors. Message: $message")
        }
    }
}

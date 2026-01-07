package dk.rosswap.mobile.core.ui.components.popup

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object PopupBus {
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
        cancelText: String = "Cancel"
    ) {
        _events.emit(
            PopupEvent.Confirm(
                title = title,
                message = message,
                confirmText = confirmText,
                cancelText = cancelText
            )
        )
    }

    // non-suspending convenience (may drop if buffer full)
    fun postSuccess(message: String, title: String = "Success") {
        _events.tryEmit(PopupEvent.Success(title = title, message = message))
    }

    fun postError(message: String, title: String = "Error") {
        _events.tryEmit(PopupEvent.Error(title = title, message = message))
    }

    fun postInfo(message: String, title: String = "Info") {
        _events.tryEmit(PopupEvent.Info(title = title, message = message))
    }

    fun postConfirm(
        message: String,
        title: String = "Confirm",
        confirmText: String = "OK",
        cancelText: String = "Cancel"
    ) {
        _events.tryEmit(
            PopupEvent.Confirm(
                title = title,
                message = message,
                confirmText = confirmText,
                cancelText = cancelText
            )
        )
    }
}

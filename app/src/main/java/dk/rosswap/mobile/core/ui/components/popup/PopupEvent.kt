package dk.rosswap.mobile.core.ui.components.popup

sealed class PopupEvent {
    data class Success(val title: String = "Success", val message: String) : PopupEvent()
    data class Error(val title: String = "Error", val message: String) : PopupEvent()
    data class Info(val title: String = "Info", val message: String) : PopupEvent()
    data class Confirm(
        val title: String = "Confirm",
        val message: String,
        val confirmText: String = "OK",
        val cancelText: String = "Cancel"
    ) : PopupEvent()
}
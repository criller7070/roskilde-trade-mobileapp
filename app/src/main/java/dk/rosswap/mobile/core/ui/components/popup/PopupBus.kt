package dk.rosswap.mobile.core.ui.components.popup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton

// this file is largely modelled on the existing PopupBus and Host created
// by android studio, but could use an overhaul

private val GLOBAL_POPUP_EVENTS = MutableLiveData<PopupEvent?>()

enum class PopupType {
    INFO, WARNING, ERROR, SUCCESS
}

data class PopupEvent(
    val message: String,
    val type: PopupType
)

@Singleton
class PopupBus @Inject constructor() {
    val popupEvents: LiveData<PopupEvent?> = GLOBAL_POPUP_EVENTS

    fun showPopup(message: String, type: PopupType = PopupType.INFO) {
        GLOBAL_POPUP_EVENTS.value = PopupEvent(message, type)
    }

    fun dismissPopup() {
        GLOBAL_POPUP_EVENTS.value = null
    }

    companion object {
        // We use LiveData for popups because these containers are lifecycle aware
        val events: LiveData<PopupEvent?>
            get() = GLOBAL_POPUP_EVENTS

        fun showError(message: String) {
            GLOBAL_POPUP_EVENTS.postValue(PopupEvent(message, PopupType.ERROR))
        }

        fun showSuccess(message: String) {
            GLOBAL_POPUP_EVENTS.postValue(PopupEvent(message, PopupType.SUCCESS))
        }

        fun showInfo(message: String) {
            GLOBAL_POPUP_EVENTS.postValue(PopupEvent(message, PopupType.INFO))
        }

        fun dismiss() {
            GLOBAL_POPUP_EVENTS.postValue(null)
        }
    }
}

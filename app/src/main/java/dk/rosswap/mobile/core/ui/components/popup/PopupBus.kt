package dk.rosswap.mobile.core.ui.components.popup

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Bus for managing popup notifications across the application.
 * Provides centralized management of popup messages and alerts.
 */
@HiltViewModel
class PopupBus @Inject constructor() {
    private val _popupEvents = MutableLiveData<PopupEvent?>()
    val popupEvents: LiveData<PopupEvent?> = _popupEvents

    fun showPopup(message: String, type: PopupType = PopupType.INFO) {
        _popupEvents.value = PopupEvent(message, type)
    }

    fun dismissPopup() {
        _popupEvents.value = null
    }

    enum class PopupType {
        INFO, WARNING, ERROR, SUCCESS
    }

    data class PopupEvent(
        val message: String,
        val type: PopupType
    )
}

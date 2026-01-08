package dk.rosswap.mobile.core.ui.reportbug

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReportBugViewModel : ViewModel() {

    private val _description = MutableLiveData("")
    val description: LiveData<String> = _description

    private val _imageUri = MutableLiveData<Uri?>(null)
    val imageUri: LiveData<Uri?> = _imageUri

    private val _isSubmitEnabled = MutableLiveData(false)
    val isSubmitEnabled: LiveData<Boolean> = _isSubmitEnabled

    fun onDescriptionChanged(text: String) {
        _description.value = text
        _isSubmitEnabled.value = text.isNotBlank()
    }

    fun setImage(uri: Uri?) {
        _imageUri.value = uri
    }

    fun submitBugReport() {
        // TODO:
        // - Send description
        // - Upload image if exists
        // - Attach metadata (device, version, etc.)
    }
}

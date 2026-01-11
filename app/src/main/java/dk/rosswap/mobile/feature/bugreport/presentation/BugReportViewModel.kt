package dk.rosswap.mobile.feature.bugreport.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.feature.bugreport.domain.ReportBugUseCase
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class BugReportViewModel @Inject constructor(
    private val reportBugUseCase: ReportBugUseCase
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _submitResult = MutableLiveData<Result<Unit>>()
    val submitResult: LiveData<Result<Unit>> = _submitResult

    private val isSubmitting = AtomicBoolean(false)

    fun submitBug(description: String, imageUri: String?) {
        if (!isSubmitting.compareAndSet(false, true)) return

        viewModelScope.launch {
            try {
                _isLoading.value = true
                val result = reportBugUseCase(description, imageUri)
                _submitResult.value = result
            } finally {
                _isLoading.value = false
                isSubmitting.set(false)
            }
        }
    }
}

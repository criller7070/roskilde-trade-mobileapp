package dk.rosswap.mobile.feature.bugreport.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.feature.bugreport.domain.SubmitBugReportUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BugReportViewModel @Inject constructor(
    private val submitBugReportUseCase: SubmitBugReportUseCase
) : ViewModel() {

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _submitResult = MutableLiveData<Result<Unit>>()
    val submitResult: LiveData<Result<Unit>> = _submitResult

    fun submitBug(description: String, imageUri: String?) {
        if (_isLoading.value == true) return

        _isLoading.value = true
        viewModelScope.launch {
            val result = submitBugReportUseCase(description, imageUri)
            _submitResult.value = result
            _isLoading.value = false
        }
    }
}

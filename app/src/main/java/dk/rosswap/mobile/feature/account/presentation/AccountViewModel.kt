package dk.rosswap.mobile.feature.account.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.feature.account.domain.CreateAccountUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val createAccountUseCase: CreateAccountUseCase
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private val _createResult = MutableLiveData<Result<Unit>>()
    val createResult: LiveData<Result<Unit>> = _createResult

    fun createAccount(name: String, email: String, password: String, acceptedTerms: Boolean) {
        if (_isLoading.value == true) return

        _isLoading.postValue(true)
        viewModelScope.launch {
            val result = createAccountUseCase(name, email, password, acceptedTerms)
            _createResult.postValue(result)
            _isLoading.postValue(false)
        }
    }

    fun onAccountCreated() {
        viewModelScope.launch {
            PopupBus.showSuccess("Your account was created.")
        }
    }

    fun onAccountError(msg: String) {
        viewModelScope.launch {
            PopupBus.showError(msg)
        }
    }

}
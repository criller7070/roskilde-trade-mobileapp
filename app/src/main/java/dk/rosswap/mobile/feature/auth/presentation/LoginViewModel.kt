package dk.rosswap.mobile.feature.auth.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.core.ui.components.popup.PopupBus
import dk.rosswap.mobile.feature.auth.domain.LoginUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _loginResult = MutableLiveData<Result<Unit>>()
    val loginResult: LiveData<Result<Unit>> = _loginResult

    fun login(email: String, password: String) {
        if (_isLoading.value == true) return

        _isLoading.postValue(true)
        viewModelScope.launch {
            val result = loginUseCase(email, password)
            _loginResult.postValue(result)
            _isLoading.postValue(false)
        }
    }

    fun onLoginSuccess() {
        viewModelScope.launch {
            PopupBus.showSuccess("Login successful.")
        }
    }

    fun onLoginError(msg: String) {
        viewModelScope.launch {
            PopupBus.showError(msg)
        }
    }
}

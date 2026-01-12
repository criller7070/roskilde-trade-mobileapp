package dk.rosswap.mobile.feature.liked.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dk.rosswap.mobile.feature.items.domain.Item
import dk.rosswap.mobile.feature.liked.domain.GetDislikedItemsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DislikedViewModel @Inject constructor(
    private val getDislikedItemsUseCase: GetDislikedItemsUseCase
) : ViewModel() {

    private val _dislikedItems = MutableStateFlow<List<Item>>(emptyList())
    val dislikedItems: StateFlow<List<Item>> = _dislikedItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadDislikedItems()
    }

    fun loadDislikedItems() {
        viewModelScope.launch {
            _isLoading.value = true
            getDislikedItemsUseCase()
                .onSuccess { _dislikedItems.value = it }
                .onFailure { /* Handle error silently or expose via state */ }
            _isLoading.value = false
        }
    }
}

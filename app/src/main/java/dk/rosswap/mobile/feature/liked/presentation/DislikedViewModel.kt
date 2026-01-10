package dk.rosswap.mobile.feature.liked.presentation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DislikedViewModel @Inject constructor() : ViewModel() {

    private val _dislikedPosts = MutableLiveData<List<Any>>(emptyList())
    val dislikedPosts: LiveData<List<Any>> = _dislikedPosts
}
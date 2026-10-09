package com.celato.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreateState(val posting: Boolean = false, val error: String? = null, val done: Boolean = false)

class CreatePostViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = PostRepository()

    private val _state = MutableStateFlow(CreateState())
    val state: StateFlow<CreateState> = _state.asStateFlow()

    fun post(user: User, imageUri: Uri, caption: String) {
        viewModelScope.launch {
            _state.value = CreateState(posting = true)
            runCatching {
                repo.createPost(getApplication<Application>().contentResolver, user, imageUri, caption.trim())
            }
                .onSuccess { _state.value = CreateState(done = true) }
                .onFailure { _state.value = CreateState(error = it.message ?: "Upload fail ho gaya") }
        }
    }

    fun reset() {
        _state.value = CreateState()
    }
}

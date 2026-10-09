package com.celato.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class Comment(val id: String, val author: User, val text: String, val createdAt: Long)

class CommentsViewModel : ViewModel() {

    private val repo = PostRepository()
    private val postId = MutableStateFlow<String?>(null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val comments: StateFlow<List<Comment>> = postId
        .filterNotNull()
        .flatMapLatest { repo.observeComments(it) }
        .catch { _error.value = it.message; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun load(id: String) {
        postId.value = id
    }

    fun send(user: User, text: String) {
        val id = postId.value ?: return
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            runCatching { repo.addComment(id, user, clean) }
                .onFailure { _error.value = it.message ?: "Comment nahi ja paya" }
        }
    }
}

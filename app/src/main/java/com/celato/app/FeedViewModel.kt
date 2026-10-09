package com.celato.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class User(val id: String, val name: String, val handle: String)

data class Post(
    val id: String,
    val author: User,
    val caption: String,
    val imageUrl: String,
    val likedBy: List<String>,
    val comments: Int,
    val createdAt: Long,
    val imageBytes: ByteArray? = null
) {
    val likes: Int get() = likedBy.size
}

class FeedViewModel : ViewModel() {

    private val repo = PostRepository()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val posts: StateFlow<List<Post>> = repo.observePosts()
        .catch { _error.value = it.message; emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggleLike(post: Post, uid: String) {
        viewModelScope.launch {
            runCatching { repo.toggleLike(post.id, uid, post.likedBy.contains(uid)) }
                .onFailure { _error.value = it.message }
        }
    }
}

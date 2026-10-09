package com.celato.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val postRepo = PostRepository()
    private val userRepo = UserRepository()

    private val target = MutableStateFlow<String?>(null)
    private val viewer = MutableStateFlow<String?>(null)

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val posts: StateFlow<List<Post>> = target
        .filterNotNull()
        .flatMapLatest { postRepo.observePostsByUser(it) }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val profile: StateFlow<UserProfile?> = target
        .filterNotNull()
        .flatMapLatest { userRepo.observeUser(it) }
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val isFollowing: StateFlow<Boolean> = combine(viewer.filterNotNull(), target.filterNotNull()) { me, t -> me to t }
        .flatMapLatest { (me, t) -> userRepo.observeIsFollowing(me, t) }
        .catch { emit(false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun load(targetId: String, viewerId: String) {
        target.value = targetId
        viewer.value = viewerId
    }

    fun toggleFollow() {
        val me = viewer.value ?: return
        val t = target.value ?: return
        if (me == t) return
        viewModelScope.launch {
            runCatching {
                if (isFollowing.value) userRepo.unfollow(me, t) else userRepo.follow(me, t)
            }.onFailure { _error.value = "Follow nahi ho paya, dobara try karo" }
        }
    }
}

package com.celato.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

data class ExploreState(
    val users: List<UserProfile> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null
)

class ExploreViewModel : ViewModel() {

    private val repo = UserRepository()
    private val query = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val state: StateFlow<ExploreState> = query
        .debounce(300)
        .distinctUntilChanged()
        .mapLatest { q ->
            runCatching { repo.searchUsers(q) }.fold(
                onSuccess = { ExploreState(users = it, loading = false) },
                onFailure = { ExploreState(loading = false, error = it.message ?: "Search fail ho gaya") }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExploreState())

    fun onQueryChange(q: String) {
        query.value = q
    }
}

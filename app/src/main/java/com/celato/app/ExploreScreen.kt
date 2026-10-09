package com.celato.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExploreScreen(vm: ExploreViewModel, me: User, onOpenProfile: (User) -> Unit) {
    val state by vm.state.collectAsState()
    var text by rememberSaveable { mutableStateOf("") }
    val results = state.users.filter { it.user.id != me.id }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Explore", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(
            value = text,
            onValueChange = { text = it; vm.onQueryChange(it) },
            placeholder = { Text("Naam ya @handle search karo") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        )

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        LazyColumn(Modifier.fillMaxSize()) {
            if (!state.loading && results.isEmpty() && state.error == null) {
                item { Text("Koi user nahi mila.", Modifier.padding(vertical = 16.dp)) }
            }
            items(results, key = { it.user.id }) { profile ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProfile(profile.user) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(profile.user, size = 48)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(profile.user.name, fontWeight = FontWeight.SemiBold)
                        Text("@${profile.user.handle} · ${profile.followers} followers", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

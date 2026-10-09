package com.celato.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * Apni profile: onLogout dena, onBack null.
 * Dusre ka profile: onBack dena, onLogout null (Follow button dikhega).
 */
@Composable
fun ProfileScreen(
    user: User,
    me: User,
    vm: ProfileViewModel,
    onLogout: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null
) {
    LaunchedEffect(user.id, me.id) { vm.load(user.id, me.id) }
    val posts by vm.posts.collectAsState()
    val profile by vm.profile.collectAsState()
    val isFollowing by vm.isFollowing.collectAsState()
    val error by vm.error.collectAsState()
    val isMe = user.id == me.id

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth()) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Avatar(user, size = 96)
                    Text(user.name, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    Text("@${user.handle}")

                    Row(
                        Modifier.fillMaxWidth().padding(top = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Stat("Posts", posts.size)
                        Stat("Followers", profile?.followers ?: 0)
                        Stat("Following", profile?.following ?: 0)
                    }

                    if (!isMe) {
                        val mod = Modifier.padding(top = 16.dp).fillMaxWidth()
                        if (isFollowing) {
                            OutlinedButton(onClick = vm::toggleFollow, modifier = mod) { Text("Following") }
                        } else {
                            Button(onClick = vm::toggleFollow, modifier = mod) { Text("Follow") }
                        }
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }

                    if (onLogout != null) {
                        OutlinedButton(onClick = onLogout, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Log out")
                        }
                    }
                }
            }
        }

        if (posts.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Abhi tak koi post nahi.", Modifier.padding(24.dp))
            }
        }

        items(posts, key = { it.id }) { post ->
            PostImage(post, Modifier.aspectRatio(1f))
        }
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(label, fontSize = 12.sp)
    }
}

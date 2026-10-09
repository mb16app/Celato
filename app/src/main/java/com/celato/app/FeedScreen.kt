package com.celato.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

private val CelatoGradient = Brush.linearGradient(
    listOf(Color(0xFF7C4DFF), Color(0xFFFF4081), Color(0xFFFFAB40))
)

@Composable
fun FeedScreen(vm: FeedViewModel, user: User, onOpenProfile: (User) -> Unit) {
    val uid = user.id
    var commentsFor by remember { mutableStateOf<String?>(null) }
    val posts by vm.posts.collectAsState()
    val error by vm.error.collectAsState()
    val authors = posts.map { it.author }.distinctBy { it.id }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text(
                "Celato",
                modifier = Modifier.padding(16.dp),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        error?.let {
            item { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp)) }
        }
        if (authors.isNotEmpty()) item { StoriesRow(authors) }
        if (posts.isEmpty() && error == null) {
            item {
                Text(
                    "Abhi koi post nahi hai. Create tab se pehli post karo!",
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        items(posts, key = { it.id }) { post ->
            PostCard(
                post,
                liked = post.likedBy.contains(uid),
                onLike = { vm.toggleLike(post, uid) },
                onComment = { commentsFor = post.id },
                onAuthorClick = { onOpenProfile(post.author) }
            )
        }
    }

    commentsFor?.let { postId ->
        CommentsSheet(postId, user, onDismiss = { commentsFor = null })
    }
}

@Composable
fun StoriesRow(users: List<User>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users, key = { it.id }) { user ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(user, size = 64, ring = true)
                Spacer(Modifier.size(4.dp))
                Text(user.handle, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun Avatar(user: User, size: Int, ring: Boolean = false) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .then(if (ring) Modifier.border(3.dp, CelatoGradient, CircleShape) else Modifier)
            .padding(if (ring) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            user.name.firstOrNull()?.uppercase() ?: "?",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
fun PostCard(post: Post, liked: Boolean, onLike: () -> Unit, onComment: () -> Unit, onAuthorClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            Modifier.clickable(onClick = onAuthorClick).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(post.author, size = 36)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(post.author.name, fontWeight = FontWeight.SemiBold)
                Text("@${post.author.handle} · ${timeAgo(post.createdAt)}", fontSize = 12.sp)
            }
        }

        PostImage(post, Modifier.fillMaxWidth().aspectRatio(1f))

        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onLike) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (liked) Color(0xFFE91E63) else MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onComment) {
                Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = "Comment")
            }
            IconButton(onClick = { /* TODO: share */ }) {
                Icon(Icons.Outlined.Share, contentDescription = "Share")
            }
        }

        Text(
            "${post.likes} likes · ${post.comments} comments",
            Modifier.padding(horizontal = 12.dp),
            fontWeight = FontWeight.Medium
        )
        if (post.caption.isNotBlank()) {
            Text(post.caption, Modifier.padding(horizontal = 12.dp, vertical = 2.dp))
        }
    }
}

private fun timeAgo(millis: Long): String {
    val minutes = (System.currentTimeMillis() - millis) / 60_000
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        minutes < 1_440 -> "${minutes / 60}h"
        else -> "${minutes / 1_440}d"
    }
}

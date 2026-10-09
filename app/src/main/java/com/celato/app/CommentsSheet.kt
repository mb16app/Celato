package com.celato.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    postId: String,
    user: User,
    onDismiss: () -> Unit,
    vm: CommentsViewModel = viewModel(key = "comments_$postId")
) {
    LaunchedEffect(postId) { vm.load(postId) }
    val comments by vm.comments.collectAsState()
    val error by vm.error.collectAsState()
    var text by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.7f).imePadding()) {
            Text(
                "Comments",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                if (comments.isEmpty()) {
                    item { Text("Abhi koi comment nahi. Pehla comment tum karo!", Modifier.padding(vertical = 16.dp)) }
                }
                items(comments, key = { it.id }) { c ->
                    Row(Modifier.padding(vertical = 8.dp)) {
                        Avatar(c.author, size = 32)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(c.author.handle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(c.text)
                        }
                    }
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }

            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(500) },
                    placeholder = { Text("Comment likho...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                IconButton(
                    onClick = { vm.send(user, text); text = "" },
                    enabled = text.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

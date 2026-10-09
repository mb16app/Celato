package com.celato.app

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun CreatePostScreen(user: User, vm: CreatePostViewModel, onPosted: () -> Unit) {
    val state by vm.state.collectAsState()

    var imageUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var caption by rememberSaveable { mutableStateOf("") }

    // Android Photo Picker: koi storage permission nahi chahiye
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) imageUri = uri
    }

    LaunchedEffect(state.done) {
        if (state.done) {
            imageUri = null
            caption = ""
            vm.reset()
            onPosted()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Nayi post", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = !state.posting) {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center
        ) {
            if (imageUri == null) {
                Text("Photo choose karne ke liye tap karo")
            } else {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Selected photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = caption,
            onValueChange = { caption = it },
            label = { Text("Caption likho...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 5,
            enabled = !state.posting
        )

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { imageUri?.let { vm.post(user, it, caption) } },
            enabled = imageUri != null && !state.posting,
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (state.posting) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            else Text("Post karo")
        }
    }
}

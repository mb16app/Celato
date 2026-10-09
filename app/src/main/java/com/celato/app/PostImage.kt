package com.celato.app

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Post ki photo dikhata hai.
 * - Free plan: photo Firestore mein bytes ke roop mein hoti hai (imageBytes)
 * - Upgrade ke baad: imageUrl se Coil load karega
 */
@Composable
fun PostImage(post: Post, modifier: Modifier = Modifier) {
    val bytes = post.imageBytes
    val placeholder = modifier.background(MaterialTheme.colorScheme.surfaceVariant)

    when {
        bytes != null -> {
            val bitmap by produceState<ImageBitmap?>(null, post.id) {
                value = withContext(Dispatchers.Default) {
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = post.caption,
                    contentScale = ContentScale.Crop,
                    modifier = modifier
                )
            } else {
                Box(placeholder)
            }
        }
        post.imageUrl.isNotBlank() -> AsyncImage(
            model = post.imageUrl,
            contentDescription = post.caption,
            contentScale = ContentScale.Crop,
            modifier = placeholder
        )
        else -> Box(placeholder)
    }
}

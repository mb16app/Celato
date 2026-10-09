package com.celato.app

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

object ImageUtils {

    /** Photo ko max 1440px tak chhota karke JPEG bytes deta hai (rotation sahi rakhta hai). */
    fun compress(resolver: ContentResolver, uri: Uri, maxSide: Int = 1440, quality: Int = 85): ByteArray {
        val bitmap = decode(resolver, uri, maxSide)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        bitmap.recycle()
        return out.toByteArray()
    }

    /** Firestore doc ki 1MB limit ke liye: photo ~450KB se neeche aane tak chhoti karta hai. */
    fun compressToLimit(resolver: ContentResolver, uri: Uri, maxBytes: Int = 450_000): ByteArray {
        var last = ByteArray(0)
        for ((side, quality) in listOf(1080 to 80, 1080 to 60, 720 to 60, 540 to 50)) {
            last = compress(resolver, uri, side, quality)
            if (last.size <= maxBytes) return last
        }
        error("Photo bohot badi hai, koi aur photo choose karo")
    }

    private fun decode(resolver: ContentResolver, uri: Uri, maxSide: Int): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder khud EXIF rotation handle karta hai
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val w = info.size.width
                val h = info.size.height
                val scale = maxSide.toFloat() / maxOf(w, h)
                if (scale < 1f) decoder.setTargetSize((w * scale).toInt(), (h * scale).toInt())
            }
        }

        // Purane Android ke liye
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, options)!! }

        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (degrees == 0f) return raw
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }
}

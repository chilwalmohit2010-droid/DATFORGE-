package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ProfileImageUtils {
    /**
     * Reads a Uri, downscales it to max 256x256 preserving aspect ratio,
     * saves to a local cache file, and returns a compact data URL (data:image/jpeg;base64,...)
     * that can be rendered anywhere via Coil and stored safely in user profile.
     */
    fun processAndSaveProfileImage(context: Context, uri: Uri, uid: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return null

            // Downscale to max 256x256
            val maxDimension = 256
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = if (width > maxDimension || height > maxDimension) {
                val ratio = width.toFloat() / height.toFloat()
                if (ratio > 1f) {
                    Pair(maxDimension, (maxDimension / ratio).toInt().coerceAtLeast(1))
                } else {
                    Pair((maxDimension * ratio).toInt().coerceAtLeast(1), maxDimension)
                }
            } else {
                Pair(width, height)
            }

            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scale.first, scale.second, true)

            // Compress to compact JPEG
            val byteStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, byteStream)
            val bytes = byteStream.toByteArray()

            // Save to local private file so it's always accessible offline on device
            val safeUid = uid.replace("[^a-zA-Z0-9_]".toRegex(), "_").ifBlank { "current" }
            val localFile = File(context.filesDir, "avatar_${safeUid}.jpg")
            FileOutputStream(localFile).use { it.write(bytes) }

            // Encode to base64 data URI (approx 12-22KB)
            val base64Str = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64Str"
        } catch (e: Exception) {
            android.util.Log.e("ProfileImageUtils", "Failed to process profile image: ${e.message}")
            null
        }
    }
}

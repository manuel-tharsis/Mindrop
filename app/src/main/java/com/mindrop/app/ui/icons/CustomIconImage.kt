package com.mindrop.app.ui.icons

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

sealed interface CustomIconLoadState {
    data object None : CustomIconLoadState
    data object Loading : CustomIconLoadState
    data object Unavailable : CustomIconLoadState
    data class Available(val bitmap: ImageBitmap) : CustomIconLoadState
}

@Composable
fun rememberCustomIcon(path: String?): State<CustomIconLoadState> = produceState(
    initialValue = if (path == null) CustomIconLoadState.None else CustomIconLoadState.Loading,
    key1 = path,
) {
    value = if (path == null) {
        CustomIconLoadState.None
    } else {
        withContext(Dispatchers.IO) {
            decodeIcon(path)?.let { bitmap ->
                CustomIconLoadState.Available(bitmap.asImageBitmap())
            } ?: CustomIconLoadState.Unavailable
        }
    }
}

private fun decodeIcon(path: String): android.graphics.Bitmap? {
    val file = File(path)
    if (!file.isFile) return null

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(file.absolutePath, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > MAX_PREVIEW_SIZE ||
        bounds.outHeight / sampleSize > MAX_PREVIEW_SIZE
    ) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply { inSampleSize = sampleSize },
    )
}

private const val MAX_PREVIEW_SIZE = 512

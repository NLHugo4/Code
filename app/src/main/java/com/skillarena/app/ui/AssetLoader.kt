package com.skillarena.app.ui

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.skillarena.app.ui.render.GameAssets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads the PNGs from graphics/png (packaged as Android assets, see app/build.gradle.kts)
 * in the background. Null until loaded; the renderer draws code-made sprites meanwhile.
 */
@Composable
fun rememberGameAssets(): State<GameAssets?> {
    val context = LocalContext.current.applicationContext
    return produceState<GameAssets?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            GameAssets.load { path ->
                val options = BitmapFactory.Options().apply {
                    // The tree border is large and only decoration: half resolution is plenty.
                    if (path == "arena/omgeving.png") inSampleSize = 2
                }
                context.assets.open(path).use { BitmapFactory.decodeStream(it, null, options) }?.asImageBitmap()
            }
        }
    }
}

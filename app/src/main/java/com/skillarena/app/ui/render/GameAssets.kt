package com.skillarena.app.ui.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

/**
 * The PNG graphics from `graphics/png` (see graphics/LEESMIJ.md), keyed by their path,
 * e.g. "units/blauw/knight.png". The images are 2x, so every pixel coordinate
 * from LEESMIJ.md is doubled here.
 */
class GameAssets(private val images: Map<String, ImageBitmap>) {
    operator fun get(path: String): ImageBitmap? = images[path]

    fun unit(cardId: String?, owner: Int) = cardId?.let { images["units/${team(owner)}/$it.png"] }
    fun card(cardId: String) = images["kaarten/$cardId.png"]
    fun spell(cardId: String) = images["spreuken/$cardId.png"]
    fun tower(isKing: Boolean, owner: Int) =
        images["torens/${if (isKing) "koningstoren" else "wachttoren"}_${team(owner)}.png"]

    companion object {
        /** Pixels per tile in the 2x arena images (LEESMIJ: 40 px per tile at 1x). */
        const val ARENA_PX_PER_TILE = 80f

        val UNIT_IDS = listOf("knight", "archers", "giant", "goblins", "musketeer", "drake", "brute", "skeletons", "ram_rider", "cannon")
        val CARD_IDS = UNIT_IDS + listOf("fireball", "arrows", "zap")

        val ALL_PATHS: List<String> = buildList {
            listOf("arena_grond", "omgeving", "water_basis", "water_glinster_a", "water_glinster_b", "brug")
                .forEach { add("arena/$it.png") }
            for (team in listOf("blauw", "rood")) {
                UNIT_IDS.forEach { add("units/$team/$it.png") }
                add("torens/koningstoren_$team.png")
                add("torens/wachttoren_$team.png")
            }
            add("torens/puin.png")
            CARD_IDS.forEach { add("kaarten/$it.png") }
            listOf("fireball", "arrows", "zap").forEach { add("spreuken/$it.png") }
            listOf("elixerbalk_leeg", "elixerbalk_vulling", "elixer_druppel").forEach { add("ui/$it.png") }
        }

        /** Loads every image with [loader]; images that fail to load are skipped (code-drawn fallback is used). */
        fun load(loader: (String) -> ImageBitmap?): GameAssets =
            GameAssets(ALL_PATHS.mapNotNull { p -> runCatching { loader(p) }.getOrNull()?.let { p to it } }.toMap())

        private fun team(owner: Int) = if (owner == 0) "blauw" else "rood"
    }
}

/**
 * Draws [image] so that its pixel [anchor] lands on screen point [at], scaled to [width] screen pixels.
 */
fun DrawScope.drawSprite(
    image: ImageBitmap,
    at: Offset,
    anchor: Offset,
    width: Float,
    alpha: Float = 1f,
    flipX: Boolean = false,
    flash: Boolean = false,
    rotationDegrees: Float = 0f,
) {
    val scale = width / image.width
    val topLeft = at - anchor * scale
    val dstOffset = IntOffset(topLeft.x.roundToInt(), topLeft.y.roundToInt())
    val dstSize = IntSize((image.width * scale).roundToInt(), (image.height * scale).roundToInt())
    withTransform({
        if (rotationDegrees != 0f) rotate(rotationDegrees, pivot = at)
        if (flipX) scale(-1f, 1f, pivot = at)
    }) {
        drawImage(image, dstOffset = dstOffset, dstSize = dstSize, alpha = alpha, filterQuality = FilterQuality.Medium)
        if (flash) {
            drawImage(
                image, dstOffset = dstOffset, dstSize = dstSize, alpha = 0.6f * alpha,
                colorFilter = ColorFilter.tint(Color.White, BlendMode.SrcAtop), filterQuality = FilterQuality.Medium,
            )
        }
    }
}
